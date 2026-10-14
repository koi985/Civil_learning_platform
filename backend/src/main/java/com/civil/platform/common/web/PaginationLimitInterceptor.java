package com.civil.platform.common.web;

import com.civil.platform.common.exception.BizException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截所有请求，校验 pageSize 不超过 100，避免恶意请求拉大 pageSize 拖垮数据库。
 */
@Component
public class PaginationLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String raw = request.getParameter("pageSize");
        if (!StringUtils.hasText(raw)) {
            return true;
        }
        try {
            long size = Long.parseLong(raw.trim());
            if (size > MAX_PAGE_SIZE) {
                throw new BizException(400, "pageSize 不能超过 " + MAX_PAGE_SIZE);
            }
            if (size <= 0) {
                throw new BizException(400, "pageSize 必须大于 0");
            }
        } catch (NumberFormatException ex) {
            throw new BizException(400, "pageSize 必须是数字");
        }
        return true;
    }
}
