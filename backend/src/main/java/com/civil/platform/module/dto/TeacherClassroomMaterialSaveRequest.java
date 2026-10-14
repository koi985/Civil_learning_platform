package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class TeacherClassroomMaterialSaveRequest {

    private Long id;

    @NotNull(message = "班级不能为空")
    private Long classroomId;

    @NotBlank(message = "材料类型不能为空")
    @Size(max = 32, message = "材料类型长度不能超过32个字符")
    private String materialCategory;

    @NotBlank(message = "材料标题不能为空")
    @Size(max = 128, message = "材料标题长度不能超过128个字符")
    private String title;

    @NotNull(message = "文件不能为空")
    private Long fileId;

    @Size(max = 500, message = "材料说明长度不能超过500个字符")
    private String description;

    @NotNull(message = "排序不能为空")
    @Min(value = 1, message = "排序至少为1")
    private Integer sortNo;

    @Size(max = 32, message = "状态长度不能超过32个字符")
    private String status;
}
