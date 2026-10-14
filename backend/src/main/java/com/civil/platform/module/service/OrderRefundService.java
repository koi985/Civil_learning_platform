package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.AdminRefundReviewRequest;
import com.civil.platform.module.dto.UserRefundRequest;
import com.civil.platform.module.entity.CourseOrder;
import com.civil.platform.module.entity.CourseOrderItem;
import com.civil.platform.module.entity.CourseOrderRefund;
import com.civil.platform.module.entity.CourseOrderRefundLog;
import com.civil.platform.module.entity.PaymentLog;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.mapper.CourseOrderItemMapper;
import com.civil.platform.module.mapper.CourseOrderMapper;
import com.civil.platform.module.mapper.CourseOrderRefundLogMapper;
import com.civil.platform.module.mapper.CourseOrderRefundMapper;
import com.civil.platform.module.mapper.PaymentLogMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderRefundService {

    private final CourseOrderMapper courseOrderMapper;
    private final CourseOrderItemMapper courseOrderItemMapper;
    private final CourseOrderRefundMapper courseOrderRefundMapper;
    private final CourseOrderRefundLogMapper courseOrderRefundLogMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final UserCourseMapper userCourseMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseLearnCountService courseLearnCountService;

    public OrderRefundService(CourseOrderMapper courseOrderMapper,
                              CourseOrderItemMapper courseOrderItemMapper,
                              CourseOrderRefundMapper courseOrderRefundMapper,
                              CourseOrderRefundLogMapper courseOrderRefundLogMapper,
                              PaymentLogMapper paymentLogMapper,
                              UserCourseMapper userCourseMapper,
                              SysUserMapper sysUserMapper,
                              CourseLearnCountService courseLearnCountService) {
        this.courseOrderMapper = courseOrderMapper;
        this.courseOrderItemMapper = courseOrderItemMapper;
        this.courseOrderRefundMapper = courseOrderRefundMapper;
        this.courseOrderRefundLogMapper = courseOrderRefundLogMapper;
        this.paymentLogMapper = paymentLogMapper;
        this.userCourseMapper = userCourseMapper;
        this.sysUserMapper = sysUserMapper;
        this.courseLearnCountService = courseLearnCountService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestRefund(Long orderId, UserRefundRequest request) {
        Long userId = SecurityUtils.getUserId();
        CourseOrder order = getOwnedPaidOrder(orderId, userId);
        CourseOrderRefund existing = loadRefundByOrderId(orderId);
        if (existing != null && !"REJECTED".equals(existing.getRefundStatus())) {
            throw new BizException("退款已在处理中或已完成");
        }
        CourseOrderRefund refund = existing == null ? new CourseOrderRefund() : existing;
        String fromStatus = refund.getRefundStatus();
        refund.setRefundNo(StringUtils.hasText(refund.getRefundNo()) ? refund.getRefundNo() : generateRefundNo());
        refund.setOrderId(orderId);
        refund.setUserId(userId);
        refund.setRefundAmount(resolveRefundAmount(order));
        refund.setRefundReason(request.getRefundReason().trim());
        refund.setRefundStatus("REQUESTED");
        refund.setRequestedAt(LocalDateTime.now());
        refund.setReviewedBy(null);
        refund.setReviewedAt(null);
        refund.setFinishedAt(null);
        refund.setReviewRemark(null);
        if (refund.getId() == null) {
            courseOrderRefundMapper.insert(refund);
        } else {
            courseOrderRefundMapper.updateById(refund);
        }
        insertRefundLog(refund, fromStatus, "REQUESTED", userId, "USER", request.getRefundReason().trim(), "REQUEST");
        return buildDetailView(refund);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long refundId, AdminRefundReviewRequest request) {
        Long operatorId = SecurityUtils.getUserId();
        CourseOrderRefund refund = getRefund(refundId);
        if (!"REQUESTED".equals(refund.getRefundStatus())) {
            throw new BizException("当前退款单不能审核通过");
        }
        transition(refund, "APPROVED", operatorId, "ADMIN", request.getReviewRemark(), "APPROVE");
        transition(refund, "REFUNDING", operatorId, "ADMIN", request.getReviewRemark(), "REFUNDING");
        CourseOrder order = courseOrderMapper.selectById(refund.getOrderId());
        if (order == null) {
            throw new BizException("退款订单不存在");
        }
        revokeCourses(order);
        order.setOrderStatus("REFUNDED");
        order.setPaymentStatus("REFUNDED");
        courseOrderMapper.updateById(order);
        insertRefundPaymentLog(refund, order);
        transition(refund, "REFUNDED", operatorId, "ADMIN", request.getReviewRemark(), "REFUND_SUCCESS");
        refund.setReviewRemark(request.getReviewRemark());
        refund.setReviewedBy(operatorId);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setFinishedAt(LocalDateTime.now());
        courseOrderRefundMapper.updateById(refund);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long refundId, AdminRefundReviewRequest request) {
        Long operatorId = SecurityUtils.getUserId();
        CourseOrderRefund refund = getRefund(refundId);
        if (!"REQUESTED".equals(refund.getRefundStatus())) {
            throw new BizException("当前退款单不能驳回");
        }
        transition(refund, "REJECTED", operatorId, "ADMIN", request.getReviewRemark(), "REJECT");
        refund.setReviewRemark(request.getReviewRemark());
        refund.setReviewedBy(operatorId);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setFinishedAt(LocalDateTime.now());
        courseOrderRefundMapper.updateById(refund);
    }

    public Map<String, Object> refundDetail(Long refundId) {
        CourseOrderRefund refund = getRefund(refundId);
        return buildDetailView(refund);
    }

    public Map<String, Object> refundByOrderId(Long orderId) {
        CourseOrderRefund refund = loadRefundByOrderId(orderId);
        return refund == null ? null : buildDetailView(refund);
    }

    public Map<String, Object> refundSummary(Long orderId) {
        CourseOrderRefund refund = loadRefundByOrderId(orderId);
        if (refund == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("refundId", refund.getId());
        map.put("refundNo", refund.getRefundNo());
        map.put("refundAmount", refund.getRefundAmount());
        map.put("refundReason", refund.getRefundReason());
        map.put("refundStatus", refund.getRefundStatus());
        map.put("requestedAt", refund.getRequestedAt());
        map.put("reviewedAt", refund.getReviewedAt());
        map.put("reviewRemark", refund.getReviewRemark());
        return map;
    }

    public List<Map<String, Object>> refundLogs(Long refundId) {
        return courseOrderRefundLogMapper.selectList(new LambdaQueryWrapper<CourseOrderRefundLog>()
                        .eq(CourseOrderRefundLog::getRefundId, refundId)
                        .orderByAsc(CourseOrderRefundLog::getCreatedAt)
                        .orderByAsc(CourseOrderRefundLog::getId))
                .stream()
                .map(this::buildRefundLogView)
                .collect(Collectors.toList());
    }

    public PageResult<Map<String, Object>> pageRefunds(Long pageNo, Long pageSize, String refundStatus, String keyword) {
        List<Map<String, Object>> all = courseOrderRefundMapper.selectList(new LambdaQueryWrapper<CourseOrderRefund>()
                        .orderByDesc(CourseOrderRefund::getRequestedAt)
                        .orderByDesc(CourseOrderRefund::getId))
                .stream()
                .map(this::buildDetailView)
                .filter(item -> !StringUtils.hasText(refundStatus) || refundStatus.equals(item.get("refundStatus")))
                .filter(item -> !StringUtils.hasText(keyword)
                        || containsIgnoreCase(item.get("refundNo"), keyword)
                        || containsIgnoreCase(item.get("orderNo"), keyword)
                        || containsIgnoreCase(item.get("nickname"), keyword))
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, Math.toIntExact((pageNo - 1) * pageSize));
        int toIndex = Math.min(all.size(), fromIndex + Math.toIntExact(pageSize));
        List<Map<String, Object>> records = fromIndex >= all.size() ? new ArrayList<>() : new ArrayList<>(all.subList(fromIndex, toIndex));
        return new PageResult<>((long) all.size(), pageNo, pageSize, records);
    }

    private void transition(CourseOrderRefund refund, String toStatus, Long operatorId, String operatorRole, String remark, String actionType) {
        String fromStatus = refund.getRefundStatus();
        refund.setRefundStatus(toStatus);
        courseOrderRefundMapper.updateById(refund);
        insertRefundLog(refund, fromStatus, toStatus, operatorId, operatorRole, remark, actionType);
    }

    private void revokeCourses(CourseOrder order) {
        List<CourseOrderItem> items = courseOrderItemMapper.selectList(new LambdaQueryWrapper<CourseOrderItem>()
                .eq(CourseOrderItem::getOrderId, order.getId()));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        userCourseMapper.delete(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getSourceOrderId, order.getId()));
        List<Long> courseIds = items.stream().map(CourseOrderItem::getCourseId).collect(Collectors.toList());
        courseLearnCountService.syncCourses(courseIds);
    }

    private void insertRefundPaymentLog(CourseOrderRefund refund, CourseOrder order) {
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setOrderId(order.getId());
        paymentLog.setUserId(order.getUserId());
        paymentLog.setPaymentChannel(StringUtils.hasText(order.getPaymentChannel()) ? order.getPaymentChannel() : "ALIPAY");
        paymentLog.setPaymentAmount(refund.getRefundAmount());
        paymentLog.setPaymentStatus("REFUNDED");
        paymentLog.setTransactionNo("REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        paymentLog.setPaidAt(LocalDateTime.now());
        paymentLogMapper.insert(paymentLog);
    }

    private void insertRefundLog(CourseOrderRefund refund, String fromStatus, String toStatus, Long operatorId, String operatorRole, String remark, String actionType) {
        SysUser operator = operatorId == null ? null : sysUserMapper.selectById(operatorId);
        CourseOrderRefundLog log = new CourseOrderRefundLog();
        log.setRefundId(refund.getId());
        log.setOrderId(refund.getOrderId());
        log.setActionType(actionType);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setOperatorName(operator == null ? operatorRole : operator.getNickname());
        log.setRemark(remark);
        courseOrderRefundLogMapper.insert(log);
    }

    private Map<String, Object> buildDetailView(CourseOrderRefund refund) {
        CourseOrder order = courseOrderMapper.selectById(refund.getOrderId());
        SysUser user = sysUserMapper.selectById(refund.getUserId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", refund.getId());
        map.put("refundNo", refund.getRefundNo());
        map.put("orderId", refund.getOrderId());
        map.put("orderNo", order == null ? null : order.getOrderNo());
        map.put("userId", refund.getUserId());
        map.put("nickname", user == null ? "未知用户" : user.getNickname());
        map.put("refundAmount", refund.getRefundAmount());
        map.put("refundReason", refund.getRefundReason());
        map.put("refundStatus", refund.getRefundStatus());
        map.put("requestedAt", refund.getRequestedAt());
        map.put("reviewedBy", refund.getReviewedBy());
        map.put("reviewedAt", refund.getReviewedAt());
        map.put("finishedAt", refund.getFinishedAt());
        map.put("reviewRemark", refund.getReviewRemark());
        map.put("createdAt", refund.getCreatedAt());
        map.put("updatedAt", refund.getUpdatedAt());
        map.put("orderStatus", order == null ? null : order.getOrderStatus());
        map.put("paymentStatus", order == null ? null : order.getPaymentStatus());
        map.put("paymentChannel", order == null ? null : order.getPaymentChannel());
        map.put("logs", refundLogs(refund.getId()));
        return map;
    }

    private Map<String, Object> buildRefundLogView(CourseOrderRefundLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("actionType", log.getActionType());
        map.put("fromStatus", log.getFromStatus());
        map.put("toStatus", log.getToStatus());
        map.put("operatorId", log.getOperatorId());
        map.put("operatorRole", log.getOperatorRole());
        map.put("operatorName", log.getOperatorName());
        map.put("remark", log.getRemark());
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    private CourseOrderRefund loadRefundByOrderId(Long orderId) {
        return courseOrderRefundMapper.selectOne(new LambdaQueryWrapper<CourseOrderRefund>()
                .eq(CourseOrderRefund::getOrderId, orderId)
                .last("LIMIT 1"));
    }

    private CourseOrderRefund getRefund(Long refundId) {
        CourseOrderRefund refund = courseOrderRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new BizException("退款单不存在");
        }
        return refund;
    }

    private CourseOrder getOwnedPaidOrder(Long orderId, Long userId) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!"PAID".equals(order.getOrderStatus())) {
            throw new BizException("当前订单不可退款");
        }
        return order;
    }

    private BigDecimal resolveRefundAmount(CourseOrder order) {
        if (order.getPayAmount() != null && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getPayAmount();
        }
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private String generateRefundNo() {
        return "RFD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private boolean containsIgnoreCase(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword.toLowerCase());
    }
}
