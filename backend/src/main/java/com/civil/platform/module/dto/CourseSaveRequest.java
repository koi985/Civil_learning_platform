package com.civil.platform.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseSaveRequest {

    private Long id;

    @NotBlank(message = "课程标题不能为空")
    private String title;

    @NotBlank(message = "请上传课程封面")
    private String coverUrl;
    private Long coverFileId;

    @NotBlank(message = "教师名称不能为空")
    private String teacherName;

    private String teacherAvatar;

    private String regionScope;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "科目不能为空")
    private String subjectName;

    @NotBlank(message = "考试类型不能为空")
    private String examType;

    @NotBlank(message = "课程类型不能为空")
    private String courseType;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @NotNull(message = "原价不能为空")
    private BigDecimal originalPrice;

    private String tags;
    private Integer learnCount;
    private String description;
    private String highlights;
    private String targetUsers;
    private Integer validityDays;
    private String status;
    private List<ChapterItem> chapters;
    private List<MaterialItem> materials;

    @Data
    public static class ChapterItem {
        private Long id;
        private String title;
        private String chapterType;
        private Integer durationMinutes;
        private Integer sortNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime liveStartTime;
        private Long videoFileId;
        private String videoUrl;
        private Boolean trialAvailable;
    }

    @Data
    public static class MaterialItem {
        private Long id;
        private String title;
        private String materialType;
        private Long fileId;
        private Integer sortNo;
    }
}
