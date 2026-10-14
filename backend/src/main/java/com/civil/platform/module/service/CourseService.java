package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.module.dto.CourseListItemResponse;
import com.civil.platform.module.dto.CourseSaveRequest;
import com.civil.platform.module.dto.CourseCommentSaveRequest;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.CourseChapter;
import com.civil.platform.module.entity.CourseComment;
import com.civil.platform.module.entity.CourseMaterial;
import com.civil.platform.module.entity.CourseOrder;
import com.civil.platform.module.entity.CourseOrderItem;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.mapper.CourseChapterMapper;
import com.civil.platform.module.mapper.CourseCommentMapper;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.CourseMaterialMapper;
import com.civil.platform.module.mapper.CourseOrderItemMapper;
import com.civil.platform.module.mapper.CourseOrderMapper;
import com.civil.platform.module.mapper.ShoppingCartItemMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import com.civil.platform.common.util.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseMaterialMapper courseMaterialMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final UserCourseMapper userCourseMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final CourseOrderItemMapper courseOrderItemMapper;
    private final ShoppingCartItemMapper shoppingCartItemMapper;
    private final RecommendEngineService recommendEngineService;

    public CourseService(CourseMapper courseMapper,
                         CourseChapterMapper courseChapterMapper,
                         CourseMaterialMapper courseMaterialMapper,
                         CourseCommentMapper courseCommentMapper,
                         SysFileRecordMapper sysFileRecordMapper,
                         SysUserMapper sysUserMapper,
                         UserCourseMapper userCourseMapper,
                         CourseOrderMapper courseOrderMapper,
                         CourseOrderItemMapper courseOrderItemMapper,
                         ShoppingCartItemMapper shoppingCartItemMapper,
                         RecommendEngineService recommendEngineService) {
        this.courseMapper = courseMapper;
        this.courseChapterMapper = courseChapterMapper;
        this.courseMaterialMapper = courseMaterialMapper;
        this.courseCommentMapper = courseCommentMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.userCourseMapper = userCourseMapper;
        this.courseOrderMapper = courseOrderMapper;
        this.courseOrderItemMapper = courseOrderItemMapper;
        this.shoppingCartItemMapper = shoppingCartItemMapper;
        this.recommendEngineService = recommendEngineService;
    }

    public PageResult<CourseListItemResponse> pageCourses(Long pageNo, Long pageSize, String keyword, String province, String subjectName, String examType, String courseType,
                                                          BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String examRegion) {
        LambdaQueryWrapper<Course> wrapper = buildCourseQuery(keyword, province, subjectName, examType, courseType, minPrice, maxPrice);
        if (isRecommendedSort(sortBy)) {
            List<Course> courses = courseMapper.selectList(wrapper);
            Map<Long, Map<String, Object>> recommendMeta = recommendEngineService.courseRecommendationMeta(examRegion);
            List<CourseListItemResponse> records = courses.stream()
                    .map(course -> buildCourseListItem(course, recommendMeta.get(course.getId())))
                    .sorted(recommendedCourseComparator())
                    .collect(Collectors.toList());
            long safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
            long safePageSize = pageSize == null || pageSize < 1 ? 8 : pageSize;
            int fromIndex = (int) Math.min((safePageNo - 1) * safePageSize, records.size());
            int toIndex = (int) Math.min(fromIndex + safePageSize, records.size());
            return new PageResult<>((long) records.size(), safePageNo, safePageSize, new ArrayList<>(records.subList(fromIndex, toIndex)));
        }
        applyCourseSort(wrapper, sortBy);
        Page<Course> page = courseMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<CourseListItemResponse> records = page.getRecords().stream()
                .map(course -> buildCourseListItem(course, null))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    private LambdaQueryWrapper<Course> buildCourseQuery(String keyword, String province, String subjectName, String examType, String courseType,
                                                        BigDecimal minPrice, BigDecimal maxPrice) {
        return new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "PUBLISHED")
                .like(StringUtils.hasText(keyword), Course::getTitle, keyword)
                .eq(StringUtils.hasText(province), Course::getProvince, province)
                .eq(StringUtils.hasText(subjectName), Course::getSubjectName, subjectName)
                .eq(StringUtils.hasText(examType), Course::getExamType, examType)
                .eq(StringUtils.hasText(courseType), Course::getCourseType, courseType)
                .ge(minPrice != null, Course::getPrice, minPrice)
                .le(maxPrice != null, Course::getPrice, maxPrice);
    }

    public Map<String, Object> detail(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        List<CourseChapter> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortNo));
        boolean freeCourse = course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0;
        long trialChapterCount = chapters.stream().filter(item -> Boolean.TRUE.equals(item.getTrialAvailable())).count();
        boolean purchased = false;
        boolean inCart = false;
        boolean canComment = false;
        if (!SecurityUtils.getRoles().isEmpty()) {
            Long userId = SecurityUtils.getUserId();
            purchased = hasActiveCourseAccess(userId, courseId, LocalDateTime.now());
            canComment = hasCommentAccess(userId, courseId, LocalDateTime.now());
            inCart = shoppingCartItemMapper.selectCount(new LambdaQueryWrapper<com.civil.platform.module.entity.ShoppingCartItem>()
                    .eq(com.civil.platform.module.entity.ShoppingCartItem::getUserId, userId)
                    .eq(com.civil.platform.module.entity.ShoppingCartItem::getCourseId, courseId)) > 0;
        }
        boolean canLearn = freeCourse || purchased;
        List<Map<String, Object>> materials = courseMaterialMapper.selectList(new LambdaQueryWrapper<CourseMaterial>()
                        .eq(CourseMaterial::getCourseId, courseId)
                        .orderByAsc(CourseMaterial::getSortNo))
                .stream()
                .map(item -> {
                    SysFileRecord fileRecord = sysFileRecordMapper.selectById(item.getFileId());
                    Map<String, Object> material = new HashMap<>();
                    material.put("id", item.getId());
                    material.put("title", item.getTitle());
                    material.put("materialType", item.getMaterialType());
                    material.put("fileUrl", canLearn && fileRecord != null ? "/api/files/view/" + fileRecord.getId() : null);
                    material.put("locked", !canLearn);
                    return material;
                }).collect(Collectors.toList());
        List<Map<String, Object>> comments = loadComments(courseId);

        List<Course> relatedCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "PUBLISHED")
                .and(wrapper -> wrapper.eq(Course::getSubjectName, course.getSubjectName())
                        .or()
                        .eq(Course::getProvince, course.getProvince())
                        .or()
                        .eq(Course::getTeacherName, course.getTeacherName()))
                .ne(Course::getId, courseId)
                .orderByDesc(Course::getLearnCount)
                .last("limit 3"));

        BigDecimal averageRating = calculateAverageRating(courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("chapters", chapters);
        result.put("materials", materials);
        result.put("comments", comments);
        result.put("relatedCourses", relatedCourses);
        result.put("commentCount", comments.size());
        result.put("averageRating", averageRating);
        result.put("purchased", purchased);
        result.put("inCart", inCart);
        result.put("freeCourse", freeCourse);
        result.put("canLearn", canLearn);
        result.put("canComment", canComment);
        result.put("canTrial", trialChapterCount > 0);
        result.put("trialChapterCount", trialChapterCount);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitComment(Long courseId, CourseCommentSaveRequest request) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !"PUBLISHED".equals(course.getStatus())) {
            throw new BizException(404, "课程不存在");
        }
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        if (!hasCommentAccess(userId, courseId, now)) {
            throw new BizException("购买或加入课程后才能评价");
        }
        CourseComment latestComment = courseCommentMapper.selectOne(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getCourseId, courseId)
                .eq(CourseComment::getUserId, userId)
                .orderByDesc(CourseComment::getCreatedAt)
                .last("limit 1"));
        if (latestComment != null
                && latestComment.getCreatedAt() != null
                && !latestComment.getCreatedAt().isBefore(now.minusDays(7))) {
            throw new BizException("同一课程 7 天内只能评价一次");
        }
        CourseComment comment = new CourseComment();
        comment.setCourseId(courseId);
        comment.setUserId(userId);
        comment.setRating(request.getRating());
        comment.setContent(request.getContent().trim());
        comment.setCreatedAt(now);
        courseCommentMapper.insert(comment);
        return buildCommentView(comment, sysUserMapper.selectById(userId));
    }

    public PageResult<Course> adminPage(Long pageNo, Long pageSize, String keyword) {
        Page<Course> page = courseMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Course>()
                .like(StringUtils.hasText(keyword), Course::getTitle, keyword)
                .orderByDesc(Course::getCreatedAt));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    public Map<String, Object> adminDetail(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("chapters", courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortNo)
                .orderByAsc(CourseChapter::getId)));
        result.put("materials", courseMaterialMapper.selectList(new LambdaQueryWrapper<CourseMaterial>()
                .eq(CourseMaterial::getCourseId, courseId)
                .orderByAsc(CourseMaterial::getSortNo)
                .orderByAsc(CourseMaterial::getId))
                .stream()
                .map(item -> {
                    Map<String, Object> material = new HashMap<>();
                    material.put("id", item.getId());
                    material.put("title", item.getTitle());
                    material.put("materialType", item.getMaterialType());
                    material.put("fileId", item.getFileId());
                    material.put("sortNo", item.getSortNo());
                    SysFileRecord file = sysFileRecordMapper.selectById(item.getFileId());
                    material.put("fileUrl", file == null ? null : "/api/files/view/" + file.getId());
                    material.put("fileName", file == null ? null : file.getOriginalName());
                    return material;
                }).collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(CourseSaveRequest request) {
        Course course = request.getId() == null ? new Course() : courseMapper.selectById(request.getId());
        if (course == null) {
            throw new BizException("课程不存在");
        }
        BeanUtils.copyProperties(request, course, "coverFileId", "coverUrl");
        applyCourseCover(course, request);
        if (request.getLearnCount() == null) {
            course.setLearnCount(0);
        }
        if (!StringUtils.hasText(course.getRegionScope())) {
            course.setRegionScope("全国".equals(course.getProvince()) ? "NATIONAL" : "PROVINCE");
        }
        if (request.getValidityDays() == null) {
            course.setValidityDays(365);
        }
        if (!StringUtils.hasText(course.getStatus())) {
            course.setStatus("PUBLISHED");
        }
        if (course.getId() == null) {
            courseMapper.insert(course);
        } else {
            courseMapper.updateById(course);
        }
        saveChapters(course.getId(), request.getChapters());
        saveMaterials(course.getId(), request.getMaterials());
    }

    private void applyCourseCover(Course course, CourseSaveRequest request) {
        if (request.getCoverFileId() != null) {
            SysFileRecord coverFile = sysFileRecordMapper.selectById(request.getCoverFileId());
            if (coverFile == null) {
                throw new BizException("课程封面文件不存在");
            }
            course.setCoverFileId(coverFile.getId());
            course.setCoverUrl("/api/files/view/" + coverFile.getId());
            return;
        }
        if (StringUtils.hasText(request.getCoverUrl())) {
            course.setCoverUrl(request.getCoverUrl());
            return;
        }
        if (!StringUtils.hasText(course.getCoverUrl())) {
            throw new BizException("请先上传课程封面");
        }
    }

    public void remove(Long id) {
        courseChapterMapper.delete(new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, id));
        courseMaterialMapper.delete(new LambdaQueryWrapper<CourseMaterial>().eq(CourseMaterial::getCourseId, id));
        courseMapper.deleteById(id);
    }

    private void saveChapters(Long courseId, List<CourseSaveRequest.ChapterItem> items) {
        courseChapterMapper.delete(new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, courseId));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        int index = 1;
        for (CourseSaveRequest.ChapterItem item : items) {
            if (!StringUtils.hasText(item.getTitle())) {
                continue;
            }
            CourseChapter chapter = new CourseChapter();
            chapter.setCourseId(courseId);
            chapter.setTitle(item.getTitle().trim());
            chapter.setChapterType(StringUtils.hasText(item.getChapterType()) ? item.getChapterType() : "RECORDED");
            chapter.setDurationMinutes(item.getDurationMinutes() == null ? 0 : item.getDurationMinutes());
            chapter.setSortNo(item.getSortNo() == null ? index : item.getSortNo());
            chapter.setLiveStartTime(item.getLiveStartTime());
            chapter.setTrialAvailable(Boolean.TRUE.equals(item.getTrialAvailable()));
            applyChapterVideo(chapter, item);
            courseChapterMapper.insert(chapter);
            index++;
        }
    }

    private void saveMaterials(Long courseId, List<CourseSaveRequest.MaterialItem> items) {
        courseMaterialMapper.delete(new LambdaQueryWrapper<CourseMaterial>().eq(CourseMaterial::getCourseId, courseId));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        int index = 1;
        for (CourseSaveRequest.MaterialItem item : items) {
            if (!StringUtils.hasText(item.getTitle()) || item.getFileId() == null) {
                continue;
            }
            SysFileRecord file = sysFileRecordMapper.selectById(item.getFileId());
            if (file == null) {
                throw new BizException("课程资料文件不存在");
            }
            CourseMaterial material = new CourseMaterial();
            material.setCourseId(courseId);
            material.setTitle(item.getTitle().trim());
            material.setMaterialType(StringUtils.hasText(item.getMaterialType()) ? item.getMaterialType() : "PDF");
            material.setFileId(file.getId());
            material.setSortNo(item.getSortNo() == null ? index : item.getSortNo());
            courseMaterialMapper.insert(material);
            index++;
        }
    }

    private void applyChapterVideo(CourseChapter chapter, CourseSaveRequest.ChapterItem item) {
        if (item.getVideoFileId() != null) {
            SysFileRecord video = sysFileRecordMapper.selectById(item.getVideoFileId());
            if (video == null) {
                throw new BizException("课程视频文件不存在");
            }
            chapter.setVideoFileId(video.getId());
            chapter.setVideoUrl("/api/files/view/" + video.getId());
            return;
        }
        if (StringUtils.hasText(item.getVideoUrl())) {
            chapter.setVideoUrl(item.getVideoUrl().trim());
            chapter.setVideoFileId(null);
            return;
        }
        chapter.setVideoUrl(null);
        chapter.setVideoFileId(null);
    }

    private boolean isRecommendedSort(String sortBy) {
        return !StringUtils.hasText(sortBy) || "RECOMMENDED".equalsIgnoreCase(sortBy.trim());
    }

    private CourseListItemResponse buildCourseListItem(Course course, Map<String, Object> recommendMeta) {
        CourseListItemResponse item = new CourseListItemResponse();
        BeanUtils.copyProperties(course, item);
        boolean recommended = recommendMeta != null && Boolean.TRUE.equals(recommendMeta.get("recommended"));
        item.setRecommended(recommended);
        item.setRecommendReason(recommended ? nullableString(recommendMeta.get("recommendReason")) : null);
        item.setExplainTags(recommended ? castStringList(recommendMeta.get("explainTags")) : new ArrayList<>());
        item.setFinalScore(recommended ? toBigDecimal(recommendMeta.get("finalScore")) : BigDecimal.ZERO);
        item.setRecommendActionText(recommended ? nullableString(recommendMeta.get("recommendActionText")) : null);
        return item;
    }

    private String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<String> castStringList(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        return ((List<?>) value).stream()
                .filter(item -> item != null && StringUtils.hasText(String.valueOf(item)))
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Comparator<CourseListItemResponse> recommendedCourseComparator() {
        return Comparator
                .comparing((CourseListItemResponse item) -> Boolean.TRUE.equals(item.getRecommended()) ? 1 : 0, Comparator.reverseOrder())
                .thenComparing(CourseListItemResponse::getFinalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getLearnCount() == null ? 0 : item.getLearnCount(), Comparator.reverseOrder())
                .thenComparing(CourseListItemResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private void applyCourseSort(LambdaQueryWrapper<Course> wrapper, String sortBy) {
        String normalizedSort = StringUtils.hasText(sortBy) ? sortBy.trim().toUpperCase() : "POPULAR";
        switch (normalizedSort) {
            case "PRICE_ASC":
                wrapper.orderByAsc(Course::getPrice).orderByDesc(Course::getLearnCount).orderByDesc(Course::getCreatedAt);
                break;
            case "PRICE_DESC":
                wrapper.orderByDesc(Course::getPrice).orderByDesc(Course::getLearnCount).orderByDesc(Course::getCreatedAt);
                break;
            case "LATEST":
                wrapper.orderByDesc(Course::getCreatedAt).orderByDesc(Course::getLearnCount);
                break;
            default:
                wrapper.orderByDesc(Course::getLearnCount).orderByDesc(Course::getCreatedAt);
                break;
        }
    }

    private List<Map<String, Object>> loadComments(Long courseId) {
        List<CourseComment> commentList = courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getCourseId, courseId)
                .orderByDesc(CourseComment::getCreatedAt)
                .orderByDesc(CourseComment::getId));
        Set<Long> userIds = commentList.stream().map(CourseComment::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> left));
        return commentList.stream()
                .map(item -> buildCommentView(item, userMap.get(item.getUserId())))
                .sorted(Comparator.comparing(comment -> (LocalDateTime) comment.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildCommentView(CourseComment item, SysUser user) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("id", item.getId());
        comment.put("rating", item.getRating());
        comment.put("content", item.getContent());
        comment.put("createdAt", item.getCreatedAt());
        comment.put("nickname", user == null || !StringUtils.hasText(user.getNickname()) ? "学员" : user.getNickname());
        comment.put("avatarUrl", user == null ? null : user.getAvatarUrl());
        return comment;
    }

    private boolean hasCommentAccess(Long userId, Long courseId, LocalDateTime now) {
        return hasActiveCourseAccess(userId, courseId, now) || hasPaidCourseOrder(userId, courseId);
    }

    private boolean hasActiveCourseAccess(Long userId, Long courseId, LocalDateTime now) {
        return userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .eq(UserCourse::getCourseId, courseId)
                .and(wrapper -> wrapper.isNull(UserCourse::getExpireAt)
                        .or()
                        .gt(UserCourse::getExpireAt, now))) > 0;
    }

    private boolean hasPaidCourseOrder(Long userId, Long courseId) {
        List<Long> paidOrderIds = courseOrderMapper.selectList(new LambdaQueryWrapper<CourseOrder>()
                        .select(CourseOrder::getId)
                        .eq(CourseOrder::getUserId, userId)
                        .eq(CourseOrder::getOrderStatus, "PAID")
                        .eq(CourseOrder::getPaymentStatus, "SUCCESS"))
                .stream()
                .map(CourseOrder::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paidOrderIds)) {
            return false;
        }
        return courseOrderItemMapper.selectCount(new LambdaQueryWrapper<CourseOrderItem>()
                .in(CourseOrderItem::getOrderId, paidOrderIds)
                .eq(CourseOrderItem::getCourseId, courseId)) > 0;
    }

    private BigDecimal calculateAverageRating(Long courseId) {
        List<CourseComment> commentList = courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .select(CourseComment::getRating)
                .eq(CourseComment::getCourseId, courseId));
        if (commentList.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = commentList.stream()
                .map(item -> BigDecimal.valueOf(item.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(commentList.size()), 1, java.math.RoundingMode.HALF_UP);
    }
}
