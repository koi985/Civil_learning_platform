package com.civil.platform.common.aspect;

import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.common.util.JsonUtils;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.entity.SysOperationLog;
import com.civil.platform.module.mapper.SysOperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final SysOperationLogMapper sysOperationLogMapper;

    public OperationLogAspect(SysOperationLogMapper sysOperationLogMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        SysOperationLog log = new SysOperationLog();
        log.setModuleName(operationLog.module());
        log.setOperationName(operationLog.name());
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUri(request.getRequestURI());
        }
        try {
            log.setOperatorId(SecurityUtils.getUserId());
            log.setOperatorName(SecurityUtils.getUsername());
            List<String> roles = SecurityUtils.getRoles();
            log.setOperatorRole(String.join(",", roles));
        } catch (Exception ignored) {
        }
        log.setRequestBody(JsonUtils.toJson(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            log.setSuccess(Boolean.TRUE);
            log.setResponseBody(JsonUtils.toJson(result));
            return result;
        } catch (Exception exception) {
            log.setSuccess(Boolean.FALSE);
            log.setErrorMessage(truncateErrorMessage(buildErrorMessage(exception)));
            throw exception;
        } finally {
            try {
                sysOperationLogMapper.insert(log);
            } catch (Exception loggingException) {
                OperationLogAspect.log.warn("Failed to persist operation log for {} {}", log.getRequestMethod(), log.getRequestUri(), loggingException);
            }
        }
    }

    private String buildErrorMessage(Exception exception) {
        if (exception == null) {
            return null;
        }
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return exception.getClass().getName();
        }
        return exception.getClass().getName() + ": " + message;
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
