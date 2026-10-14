package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class CourseLearnCountService {

    private final CourseMapper courseMapper;
    private final UserCourseMapper userCourseMapper;

    public CourseLearnCountService(CourseMapper courseMapper, UserCourseMapper userCourseMapper) {
        this.courseMapper = courseMapper;
        this.userCourseMapper = userCourseMapper;
    }

    public void syncCourse(Long courseId) {
        if (courseId == null) {
            return;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return;
        }
        Number count = (Number) userCourseMapper.selectObjs(new QueryWrapper<UserCourse>()
                .select("COUNT(DISTINCT user_id)")
                .eq("course_id", courseId))
                .stream()
                .findFirst()
                .orElse(0L);
        course.setLearnCount(count == null ? 0 : count.intValue());
        courseMapper.updateById(course);
    }

    public void syncCourses(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }
        Set<Long> distinctIds = new HashSet<>(courseIds);
        distinctIds.forEach(this::syncCourse);
    }

    public void syncAllCourses() {
        courseMapper.selectList(null).forEach(course -> syncCourse(course.getId()));
    }
}
