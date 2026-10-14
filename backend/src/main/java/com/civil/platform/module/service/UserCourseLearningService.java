package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.CourseLearningProgressRequest;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.CourseChapter;
import com.civil.platform.module.entity.CourseMaterial;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.entity.UserCourseLearningRecord;
import com.civil.platform.module.entity.UserCourseChapterProgress;
import com.civil.platform.module.entity.UserCourseStudyProgress;
import com.civil.platform.module.mapper.CourseChapterMapper;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.CourseMaterialMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.UserCourseChapterProgressMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import com.civil.platform.module.mapper.UserCourseLearningRecordMapper;
import com.civil.platform.module.mapper.UserCourseStudyProgressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserCourseLearningService {

    private final OrderService orderService;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseMaterialMapper courseMaterialMapper;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserCourseLearningRecordMapper userCourseLearningRecordMapper;
    private final UserCourseStudyProgressMapper userCourseStudyProgressMapper;
    private final UserCourseChapterProgressMapper userCourseChapterProgressMapper;
    private final RecommendEngineService recommendEngineService;
    private final CourseLearnCountService courseLearnCountService;

    public UserCourseLearningService(OrderService orderService,
                                     CourseMapper courseMapper,
                                     CourseChapterMapper courseChapterMapper,
                                     CourseMaterialMapper courseMaterialMapper,
                                     SysFileRecordMapper sysFileRecordMapper,
                                     UserCourseMapper userCourseMapper,
                                     UserCourseLearningRecordMapper userCourseLearningRecordMapper,
                                     UserCourseStudyProgressMapper userCourseStudyProgressMapper,
                                     UserCourseChapterProgressMapper userCourseChapterProgressMapper,
                                     RecommendEngineService recommendEngineService,
                                     CourseLearnCountService courseLearnCountService) {
        this.orderService = orderService;
        this.courseMapper = courseMapper;
        this.courseChapterMapper = courseChapterMapper;
        this.courseMaterialMapper = courseMaterialMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.userCourseMapper = userCourseMapper;
        this.userCourseLearningRecordMapper = userCourseLearningRecordMapper;
        this.userCourseStudyProgressMapper = userCourseStudyProgressMapper;
        this.userCourseChapterProgressMapper = userCourseChapterProgressMapper;
        this.recommendEngineService = recommendEngineService;
        this.courseLearnCountService = courseLearnCountService;
    }

    public List<Map<String, Object>> myCourses() {
        return orderService.myCourses();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> claimFreeCourse(Long courseId) {
        Long userId = SecurityUtils.getUserId();
        Course course = courseMapper.selectById(courseId);
        if (course == null || !"PUBLISHED".equals(course.getStatus())) {
            throw new BizException(404, "课程不存在");
        }
        if (!isFreeCourse(course)) {
            throw new BizException("当前课程不是免费课");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", course.getId());
        result.put("claimed", ensureCourseAccessRecord(userId, course));
        result.put("freeCourse", true);
        return result;
    }

    public Map<String, Object> courseLearning(Long courseId) {
        Long userId = SecurityUtils.getUserId();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        List<CourseChapter> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortNo)
                .orderByAsc(CourseChapter::getId));
        boolean fullAccess = hasFullCourseAccess(userId, course);
        if (!fullAccess && isFreeCourse(course)) {
            ensureCourseAccessRecord(userId, course);
            fullAccess = true;
        }
        if (!fullAccess) {
            List<CourseChapter> trialChapters = chapters.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getTrialAvailable()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(trialChapters)) {
                throw new BizException(403, "请先购买课程");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("course", course);
            result.put("chapters", trialChapters.stream()
                    .map(item -> buildChapterLearningItem(item, null))
                    .collect(Collectors.toList()));
            result.put("materials", new ArrayList<>());
            result.put("studyProgress", buildPreviewStudyProgress(trialChapters, chapters.size()));
            result.put("recentLearningRecords", new ArrayList<>());
            result.put("accessMode", "TRIAL");
            result.put("canRecordProgress", false);
            result.put("purchaseRequired", true);
            result.put("trialChapterCount", trialChapters.size());
            result.put("fullChapterCount", chapters.size());
            result.put("previewMessage", "当前为试听模式，仅开放试听章节。购买后可解锁完整课程、资料与学习进度记录。");
            return result;
        }

        List<UserCourseChapterProgress> chapterProgressList = loadChapterProgress(userId, courseId);
        Map<Long, UserCourseChapterProgress> chapterProgressMap = chapterProgressList.stream()
                .collect(Collectors.toMap(UserCourseChapterProgress::getChapterId, item -> item, (left, right) -> left));

        List<Map<String, Object>> materials = courseMaterialMapper.selectList(new LambdaQueryWrapper<CourseMaterial>()
                        .eq(CourseMaterial::getCourseId, courseId)
                        .orderByAsc(CourseMaterial::getSortNo)
                        .orderByAsc(CourseMaterial::getId))
                .stream()
                .map(item -> {
                    SysFileRecord file = sysFileRecordMapper.selectById(item.getFileId());
                    Map<String, Object> material = new HashMap<>();
                    material.put("id", item.getId());
                    material.put("title", item.getTitle());
                    material.put("materialType", item.getMaterialType());
                    material.put("fileUrl", file == null ? null : "/api/files/view/" + file.getId());
                    return material;
                })
                .collect(Collectors.toList());

        UserCourseStudyProgress studyProgress = getOrInitStudyProgress(userId, courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("chapters", chapters.stream()
                .map(item -> buildChapterLearningItem(item, chapterProgressMap.get(item.getId())))
                .collect(Collectors.toList()));
        result.put("materials", materials);
        result.put("studyProgress", buildStudyProgress(studyProgress, chapters, chapterProgressList));
        result.put("recentLearningRecords", buildRecentLearningRecords(chapters, chapterProgressList));
        result.put("accessMode", "FULL");
        result.put("canRecordProgress", true);
        result.put("purchaseRequired", false);
        result.put("trialChapterCount", chapters.stream().filter(item -> Boolean.TRUE.equals(item.getTrialAvailable())).count());
        result.put("fullChapterCount", chapters.size());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateCourseLearningProgress(Long courseId, CourseLearningProgressRequest request) {
        if (request == null || request.getChapterId() == null) {
            throw new BizException("请选择课程章节");
        }
        Long userId = SecurityUtils.getUserId();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        ensureFullCourseAccess(userId, course);

        CourseChapter chapter = courseChapterMapper.selectById(request.getChapterId());
        if (chapter == null || !courseId.equals(chapter.getCourseId())) {
            throw new BizException("章节不存在");
        }

        int watchSeconds = Math.max(request.getWatchSeconds() == null ? 0 : request.getWatchSeconds(), 0);
        LocalDateTime now = LocalDateTime.now();
        UserCourseChapterProgress chapterProgress = userCourseChapterProgressMapper.selectOne(new LambdaQueryWrapper<UserCourseChapterProgress>()
                .eq(UserCourseChapterProgress::getUserId, userId)
                .eq(UserCourseChapterProgress::getCourseId, courseId)
                .eq(UserCourseChapterProgress::getChapterId, chapter.getId())
                .last("limit 1"));
        if (chapterProgress == null) {
            chapterProgress = new UserCourseChapterProgress();
            chapterProgress.setUserId(userId);
            chapterProgress.setCourseId(courseId);
            chapterProgress.setChapterId(chapter.getId());
            chapterProgress.setStudySeconds(0);
            chapterProgress.setProgressPercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            chapterProgress.setCompleted(false);
            chapterProgress.setFirstStartedAt(now);
        }

        chapterProgress.setStudySeconds((chapterProgress.getStudySeconds() == null ? 0 : chapterProgress.getStudySeconds()) + watchSeconds);
        BigDecimal chapterPercent = calculateChapterProgressPercent(chapterProgress.getStudySeconds(), chapter.getDurationMinutes(), Boolean.TRUE.equals(request.getCompleted()));
        chapterProgress.setProgressPercent(chapterPercent);
        chapterProgress.setLastStudyAt(now);
        if (Boolean.TRUE.equals(request.getCompleted()) || chapterPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            chapterProgress.setCompleted(true);
            chapterProgress.setCompletedAt(now);
            chapterProgress.setProgressPercent(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP));
        }
        if (chapterProgress.getId() == null) {
            userCourseChapterProgressMapper.insert(chapterProgress);
        } else {
            userCourseChapterProgressMapper.updateById(chapterProgress);
        }

        UserCourseLearningRecord learningRecord = new UserCourseLearningRecord();
        learningRecord.setUserId(userId);
        learningRecord.setCourseId(courseId);
        learningRecord.setChapterId(chapter.getId());
        learningRecord.setWatchSeconds(watchSeconds);
        learningRecord.setProgressPercent(chapterProgress.getProgressPercent());
        learningRecord.setCompleted(Boolean.TRUE.equals(chapterProgress.getCompleted()));
        learningRecord.setActionType(Boolean.TRUE.equals(request.getCompleted()) ? "COMPLETE" : "STUDY");
        userCourseLearningRecordMapper.insert(learningRecord);

        List<CourseChapter> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortNo)
                .orderByAsc(CourseChapter::getId));
        List<UserCourseChapterProgress> chapterProgressList = loadChapterProgress(userId, courseId);
        long completedCount = chapterProgressList.stream().filter(item -> Boolean.TRUE.equals(item.getCompleted())).count();
        int totalStudySeconds = chapterProgressList.stream().mapToInt(item -> item.getStudySeconds() == null ? 0 : item.getStudySeconds()).sum();

        UserCourseStudyProgress studyProgress = getOrInitStudyProgress(userId, courseId);
        studyProgress.setCurrentChapterId(chapter.getId());
        studyProgress.setCompletedChapterCount((int) completedCount);
        studyProgress.setProgressPercent(calculateCourseProgressPercent(completedCount, chapters.size()));
        studyProgress.setTotalStudySeconds(totalStudySeconds);
        studyProgress.setLastStudyAt(now);
        if (studyProgress.getId() == null) {
            userCourseStudyProgressMapper.insert(studyProgress);
        } else {
            userCourseStudyProgressMapper.updateById(studyProgress);
        }

        Map<Long, UserCourseChapterProgress> chapterProgressMap = chapterProgressList.stream()
                .collect(Collectors.toMap(UserCourseChapterProgress::getChapterId, item -> item, (left, right) -> left));
        Map<String, Object> result = new HashMap<>();
        result.put("studyProgress", buildStudyProgress(studyProgress, chapters, chapterProgressList));
        result.put("chapters", chapters.stream()
                .map(item -> buildChapterLearningItem(item, chapterProgressMap.get(item.getId())))
                .collect(Collectors.toList()));
        result.put("recentLearningRecords", buildRecentLearningRecords(chapters, chapterProgressList));
        recommendEngineService.enqueueBehaviorRefresh(userId, "COURSE_STUDY");
        return result;
    }

    private void ensureFullCourseAccess(Long userId, Course course) {
        if (hasFullCourseAccess(userId, course)) {
            return;
        }
        if (isFreeCourse(course)) {
            ensureCourseAccessRecord(userId, course);
            return;
        }
        throw new BizException(403, "请先购买课程");
    }

    private boolean hasFullCourseAccess(Long userId, Course course) {
        return userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .eq(UserCourse::getCourseId, course.getId())) > 0;
    }

    private boolean isFreeCourse(Course course) {
        return course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0;
    }

    private boolean ensureCourseAccessRecord(Long userId, Course course) {
        if (hasFullCourseAccess(userId, course)) {
            return false;
        }
        UserCourse userCourse = new UserCourse();
        userCourse.setUserId(userId);
        userCourse.setCourseId(course.getId());
        userCourse.setSourceOrderId(null);
        userCourse.setExpireAt(course.getValidityDays() == null ? null : LocalDateTime.now().plusDays(course.getValidityDays()));
        userCourseMapper.insert(userCourse);
        courseLearnCountService.syncCourse(course.getId());
        recommendEngineService.enqueueBehaviorRefresh(userId, "COURSE_ACCESS");
        return true;
    }

    private UserCourseStudyProgress getOrInitStudyProgress(Long userId, Long courseId) {
        UserCourseStudyProgress progress = userCourseStudyProgressMapper.selectOne(new LambdaQueryWrapper<UserCourseStudyProgress>()
                .eq(UserCourseStudyProgress::getUserId, userId)
                .eq(UserCourseStudyProgress::getCourseId, courseId)
                .last("limit 1"));
        if (progress != null) {
            return progress;
        }
        UserCourseStudyProgress created = new UserCourseStudyProgress();
        created.setUserId(userId);
        created.setCourseId(courseId);
        created.setCompletedChapterCount(0);
        created.setProgressPercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setTotalStudySeconds(0);
        return created;
    }

    private List<UserCourseChapterProgress> loadChapterProgress(Long userId, Long courseId) {
        return userCourseChapterProgressMapper.selectList(new LambdaQueryWrapper<UserCourseChapterProgress>()
                .eq(UserCourseChapterProgress::getUserId, userId)
                .eq(UserCourseChapterProgress::getCourseId, courseId)
                .orderByDesc(UserCourseChapterProgress::getLastStudyAt)
                .orderByDesc(UserCourseChapterProgress::getUpdatedAt));
    }

    private Map<String, Object> buildStudyProgress(UserCourseStudyProgress studyProgress,
                                                   List<CourseChapter> chapters,
                                                   List<UserCourseChapterProgress> chapterProgressList) {
        long completedCount = chapterProgressList.stream().filter(item -> Boolean.TRUE.equals(item.getCompleted())).count();
        Map<String, Object> progress = new HashMap<>();
        progress.put("currentChapterId", studyProgress.getCurrentChapterId());
        progress.put("completedChapterCount", studyProgress.getCompletedChapterCount() == null ? (int) completedCount : studyProgress.getCompletedChapterCount());
        progress.put("totalChapterCount", chapters.size());
        progress.put("progressPercent", studyProgress.getProgressPercent() == null
                ? calculateCourseProgressPercent(completedCount, chapters.size())
                : studyProgress.getProgressPercent());
        progress.put("totalStudySeconds", studyProgress.getTotalStudySeconds() == null ? 0 : studyProgress.getTotalStudySeconds());
        progress.put("lastStudyAt", studyProgress.getLastStudyAt());
        progress.put("completed", !CollectionUtils.isEmpty(chapters) && completedCount >= chapters.size());
        return progress;
    }

    private Map<String, Object> buildPreviewStudyProgress(List<CourseChapter> trialChapters, int totalChapterCount) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("currentChapterId", CollectionUtils.isEmpty(trialChapters) ? null : trialChapters.get(0).getId());
        progress.put("completedChapterCount", 0);
        progress.put("totalChapterCount", totalChapterCount);
        progress.put("progressPercent", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        progress.put("totalStudySeconds", 0);
        progress.put("lastStudyAt", null);
        progress.put("completed", false);
        return progress;
    }

    private List<Map<String, Object>> buildRecentLearningRecords(List<CourseChapter> chapters,
                                                                 List<UserCourseChapterProgress> chapterProgressList) {
        Map<Long, CourseChapter> chapterMap = chapters.stream()
                .collect(Collectors.toMap(CourseChapter::getId, item -> item, (left, right) -> left));
        return chapterProgressList.stream()
                .filter(item -> item.getLastStudyAt() != null)
                .sorted(Comparator.comparing(UserCourseChapterProgress::getLastStudyAt).reversed())
                .limit(5)
                .map(item -> {
                    CourseChapter chapter = chapterMap.get(item.getChapterId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("chapterId", item.getChapterId());
                    map.put("chapterTitle", chapter == null ? "未知章节" : chapter.getTitle());
                    map.put("studySeconds", item.getStudySeconds() == null ? 0 : item.getStudySeconds());
                    map.put("progressPercent", item.getProgressPercent() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : item.getProgressPercent());
                    map.put("completed", Boolean.TRUE.equals(item.getCompleted()));
                    map.put("lastStudyAt", item.getLastStudyAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildChapterLearningItem(CourseChapter chapter, UserCourseChapterProgress progress) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", chapter.getId());
        item.put("title", chapter.getTitle());
        item.put("chapterType", chapter.getChapterType());
        item.put("durationMinutes", chapter.getDurationMinutes());
        item.put("sortNo", chapter.getSortNo());
        item.put("liveStartTime", chapter.getLiveStartTime());
        item.put("videoFileId", chapter.getVideoFileId());
        item.put("videoUrl", chapter.getVideoUrl());
        item.put("trialAvailable", chapter.getTrialAvailable());
        item.put("studySeconds", progress == null || progress.getStudySeconds() == null ? 0 : progress.getStudySeconds());
        item.put("progressPercent", progress == null || progress.getProgressPercent() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : progress.getProgressPercent());
        item.put("completed", progress != null && Boolean.TRUE.equals(progress.getCompleted()));
        item.put("lastStudyAt", progress == null ? null : progress.getLastStudyAt());
        item.put("completedAt", progress == null ? null : progress.getCompletedAt());
        return item;
    }

    private BigDecimal calculateCourseProgressPercent(long completedCount, int totalChapters) {
        if (totalChapters <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalChapters), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChapterProgressPercent(Integer studySeconds, Integer durationMinutes, boolean forceComplete) {
        if (forceComplete) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        int learnedSeconds = studySeconds == null ? 0 : studySeconds;
        int totalSeconds = Math.max((durationMinutes == null ? 0 : durationMinutes) * 60, 0);
        if (totalSeconds <= 0) {
            return learnedSeconds > 0 ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal percent = BigDecimal.valueOf(learnedSeconds)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSeconds), 2, RoundingMode.HALF_UP);
        return percent.compareTo(BigDecimal.valueOf(100)) > 0
                ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP)
                : percent;
    }
}
