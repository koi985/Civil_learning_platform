package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file_record")
public class SysFileRecord extends BaseEntity {

    private String originalName;
    private String storedName;
    private String filePath;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String bizType;
    private Long uploaderId;
    private LocalDateTime createdAt;
}
