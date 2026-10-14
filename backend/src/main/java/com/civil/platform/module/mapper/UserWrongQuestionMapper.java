package com.civil.platform.module.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.module.entity.UserWrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserWrongQuestionMapper extends BaseMapper<UserWrongQuestion> {

    @Select("<script>"
            + "SELECT uwq.* "
            + "FROM user_wrong_question uwq "
            + "JOIN question q ON q.id = uwq.question_id "
            + "WHERE uwq.user_id = #{userId} "
            + "AND uwq.removed = 0 "
            + "<if test='subjectName != null and subjectName != \"\"'>"
            + "AND q.subject_name = #{subjectName} "
            + "</if>"
            + "<if test='categoryName != null and categoryName != \"\"'>"
            + "AND q.category_name = #{categoryName} "
            + "</if>"
            + "ORDER BY uwq.last_wrong_at DESC"
            + "</script>")
    Page<UserWrongQuestion> pageActiveWrongQuestions(Page<UserWrongQuestion> page,
                                                     @Param("userId") Long userId,
                                                     @Param("subjectName") String subjectName,
                                                     @Param("categoryName") String categoryName);
}
