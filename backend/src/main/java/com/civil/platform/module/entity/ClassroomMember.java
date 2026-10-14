package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_member")
public class ClassroomMember extends BaseEntity {

    private Long classroomId;
    private Long userId;
    private String roleCode;
    private LocalDateTime joinedAt;
}
