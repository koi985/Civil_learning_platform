package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_material")
public class CourseMaterial extends BaseEntity {

    private Long courseId;
    private String title;
    private String materialType;
    private Long fileId;
    private Integer sortNo;
}
