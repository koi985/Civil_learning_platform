package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_operation_log")
public class SysOperationLog extends BaseEntity {

    private String moduleName;
    private String operationName;
    private String requestMethod;
    private String requestUri;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String requestBody;
    private String responseBody;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
}
