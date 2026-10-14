package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_question_favorite")
public class UserQuestionFavorite extends BaseEntity {

    private Long userId;
    private Long questionId;
    private LocalDateTime createdAt;
}
