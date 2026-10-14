package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_material")
public class ClassroomMaterial extends BaseEntity {

    private Long classroomId;
    private String materialCategory;
    private String title;
    private Long fileId;
    private String description;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
