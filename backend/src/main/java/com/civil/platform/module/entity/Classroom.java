package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom")
public class Classroom extends BaseEntity {

    private String name;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private Long coverFileId;
    private String coverUrl;
    private String intro;
    private Integer capacity;
    private Boolean isPublic;
    private String inviteCode;
    private Integer memberCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
