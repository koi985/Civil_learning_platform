package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.ExcelUtils;
import com.civil.platform.module.dto.AdminRoleSaveRequest;
import com.civil.platform.module.entity.CourseComment;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.CourseOrder;
import com.civil.platform.module.entity.CourseOrderItem;
import com.civil.platform.module.entity.CourseOrderRefund;
import com.civil.platform.module.entity.NewsComment;
import com.civil.platform.module.entity.News;
import com.civil.platform.module.entity.SystemBroadcastMessage;
import com.civil.platform.module.entity.SysRole;
import com.civil.platform.module.entity.SysUserRole;
import com.civil.platform.module.mapper.CourseCommentMapper;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.CourseOrderItemMapper;
import com.civil.platform.module.mapper.CourseOrderMapper;
import com.civil.platform.module.mapper.CourseOrderRefundMapper;
import com.civil.platform.module.mapper.NewsCommentMapper;
import com.civil.platform.module.mapper.NewsMapper;
import com.civil.platform.module.mapper.SystemBroadcastMessageMapper;
import com.civil.platform.module.mapper.SysRoleMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GovernanceService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final NewsCommentMapper newsCommentMapper;
    private final CourseMapper courseMapper;
    private final NewsMapper newsMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final CourseOrderItemMapper courseOrderItemMapper;
    private final CourseOrderRefundMapper courseOrderRefundMapper;
    private final SystemBroadcastMessageMapper systemBroadcastMessageMapper;

    public GovernanceService(SysRoleMapper sysRoleMapper,
                             SysUserMapper sysUserMapper,
                             SysUserRoleMapper sysUserRoleMapper,
                             CourseCommentMapper courseCommentMapper,
                             NewsCommentMapper newsCommentMapper,
                             CourseMapper courseMapper,
                             NewsMapper newsMapper,
                             CourseOrderMapper courseOrderMapper,
                             CourseOrderItemMapper courseOrderItemMapper,
                             CourseOrderRefundMapper courseOrderRefundMapper,
                             SystemBroadcastMessageMapper systemBroadcastMessageMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.courseCommentMapper = courseCommentMapper;
        this.newsCommentMapper = newsCommentMapper;
        this.courseMapper = courseMapper;
        this.newsMapper = newsMapper;
        this.courseOrderMapper = courseOrderMapper;
        this.courseOrderItemMapper = courseOrderItemMapper;
        this.courseOrderRefundMapper = courseOrderRefundMapper;
        this.systemBroadcastMessageMapper = systemBroadcastMessageMapper;
    }

    public List<Map<String, Object>> listRoles() {
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSortNo)
                .orderByAsc(SysRole::getId));
        Map<Long, Long> roleCountMap = sysUserRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleId, Collectors.counting()));
        return roles.stream().map(role -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", role.getId());
            item.put("code", role.getCode());
            item.put("name", role.getName());
            item.put("descriptionText", role.getDescriptionText());
            item.put("sortNo", role.getSortNo());
            item.put("enabled", role.getEnabled());
            item.put("userCount", roleCountMap.getOrDefault(role.getId(), 0L));
            item.put("createdAt", role.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRole(AdminRoleSaveRequest request) {
        SysRole role = request.getId() == null ? new SysRole() : sysRoleMapper.selectById(request.getId());
        if (role == null) {
            throw new BizException("角色不存在");
        }
        SysRole exists = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, request.getCode())
                .ne(request.getId() != null, SysRole::getId, request.getId())
                .last("LIMIT 1"));
        if (exists != null) {
            throw new BizException("角色编码已存在");
        }
        role.setCode(request.getCode().trim());
        role.setName(request.getName().trim());
        role.setDescriptionText(request.getDescriptionText());
        role.setSortNo(request.getSortNo());
        role.setEnabled(request.getEnabled());
        if (role.getId() == null) {
            sysRoleMapper.insert(role);
        } else {
            sysRoleMapper.updateById(role);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        if (Arrays.asList("USER", "TEACHER", "ADMIN").contains(role.getCode())) {
            throw new BizException("系统内置角色不可删除");
        }
        long userCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BizException("该角色已分配给用户，无法删除");
        }
        sysRoleMapper.deleteById(roleId);
    }

    public PageResult<Map<String, Object>> pageEvaluations(Long pageNo, Long pageSize, String source, String keyword) {
        List<Map<String, Object>> records = new ArrayList<>();
        if (!StringUtils.hasText(source) || "COURSE".equals(source)) {
            records.addAll(loadCourseEvaluations(keyword));
        }
        if (!StringUtils.hasText(source) || "NEWS".equals(source)) {
            records.addAll(loadNewsEvaluations(keyword));
        }
        records.sort((left, right) -> compareDateTime(right.get("createdAt"), left.get("createdAt")));
        int fromIndex = Math.max(0, Math.toIntExact((pageNo - 1) * pageSize));
        int toIndex = Math.min(records.size(), fromIndex + Math.toIntExact(pageSize));
        List<Map<String, Object>> pageRecords = fromIndex >= records.size() ? new ArrayList<>() : new ArrayList<>(records.subList(fromIndex, toIndex));
        return new PageResult<>((long) records.size(), pageNo, pageSize, pageRecords);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEvaluation(String source, Long id) {
        if ("COURSE".equals(source)) {
            courseCommentMapper.deleteById(id);
            return;
        }
        if ("NEWS".equals(source)) {
            newsCommentMapper.deleteById(id);
            return;
        }
        throw new BizException("不支持的评价类型");
    }

    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        List<CourseOrder> paidOrders = courseOrderMapper.selectList(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getOrderStatus, "PAID")
                .orderByDesc(CourseOrder::getPaidAt)
                .orderByDesc(CourseOrder::getId));
        List<CourseOrderRefund> refunds = courseOrderRefundMapper.selectList(new LambdaQueryWrapper<CourseOrderRefund>()
                .orderByDesc(CourseOrderRefund::getRequestedAt)
                .orderByDesc(CourseOrderRefund::getId));
        List<CourseComment> courseComments = courseCommentMapper.selectList(null);
        List<NewsComment> newsComments = newsCommentMapper.selectList(null);
        List<SystemBroadcastMessage> broadcastMessages = systemBroadcastMessageMapper.selectList(new LambdaQueryWrapper<SystemBroadcastMessage>()
                .orderByDesc(SystemBroadcastMessage::getSentAt)
                .orderByDesc(SystemBroadcastMessage::getId));

        result.put("overview", buildOverview(paidOrders, refunds, courseComments, newsComments, broadcastMessages));
        result.put("salesTrend", buildSalesTrend(paidOrders));
        result.put("orderStatusStats", buildOrderStatusStats());
        result.put("refundStatusStats", buildRefundStatusStats(refunds));
        result.put("subjectSales", buildSubjectSales(paidOrders));
        result.put("roleStats", buildRoleStats());
        result.put("commentStats", buildCommentStats(courseComments, newsComments));
        result.put("broadcastStats", buildBroadcastStats(broadcastMessages));
        return result;
    }

    public byte[] exportStatistics() {
        Map<String, Object> statistics = statistics();
        @SuppressWarnings("unchecked")
        Map<String, Object> overview = (Map<String, Object>) statistics.get("overview");
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("已支付订单", overview.get("paidOrderCount")));
        rows.add(Arrays.asList("销售额", overview.get("salesAmount")));
        rows.add(Arrays.asList("用户数", overview.get("userCount")));
        rows.add(Arrays.asList("练习提交数", overview.get("practiceCount")));
        rows.add(Arrays.asList("退款单数", overview.get("refundCount")));
        rows.add(Arrays.asList("课程评价数", overview.get("courseCommentCount")));
        rows.add(Arrays.asList("资讯评价数", overview.get("newsCommentCount")));
        rows.add(Arrays.asList("广播消息数", overview.get("broadcastCount")));
        rows.add(Arrays.asList("角色数", overview.get("roleCount")));
        rows.add(Arrays.asList("退款状态统计", ""));
        for (Map<String, Object> item : getList(statistics, "refundStatusStats")) {
            rows.add(Arrays.asList("退款状态-" + item.get("name"), item.get("value")));
        }
        rows.add(Arrays.asList("角色统计", ""));
        for (Map<String, Object> item : getList(statistics, "roleStats")) {
            rows.add(Arrays.asList(item.get("roleCode"), item.get("userCount")));
        }
        rows.add(Arrays.asList("评价统计", ""));
        for (Map<String, Object> item : getList(statistics, "commentStats")) {
            rows.add(Arrays.asList(item.get("resourceType"), item.get("count")));
        }
        rows.add(Arrays.asList("广播统计", ""));
        for (Map<String, Object> item : getList(statistics, "broadcastStats")) {
            rows.add(Arrays.asList(item.get("name"), item.get("value")));
        }
        return ExcelUtils.buildWorkbook("平台统计导出", Arrays.asList("指标", "值"), rows);
    }

    private List<Map<String, Object>> loadCourseEvaluations(String keyword) {
        List<CourseComment> comments = courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .orderByDesc(CourseComment::getCreatedAt)
                .orderByDesc(CourseComment::getId));
        Map<Long, String> courseTitleMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getId(), item -> item.getTitle(), (left, right) -> left));
        Map<Long, com.civil.platform.module.entity.SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getId(), Function.identity(), (left, right) -> left));
        return comments.stream().map(comment -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", comment.getId());
            item.put("source", "COURSE");
            item.put("resourceType", "COURSE");
            item.put("resourceId", comment.getCourseId());
            item.put("resourceTitle", courseTitleMap.get(comment.getCourseId()));
            item.put("nickname", userMap.get(comment.getUserId()) == null ? "未知用户" : userMap.get(comment.getUserId()).getNickname());
            item.put("rating", comment.getRating());
            item.put("content", comment.getContent());
            item.put("createdAt", comment.getCreatedAt());
            return item;
        }).filter(item -> !StringUtils.hasText(keyword)
                || containsIgnoreCase(item.get("resourceTitle"), keyword)
                || containsIgnoreCase(item.get("nickname"), keyword)
                || containsIgnoreCase(item.get("content"), keyword))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadNewsEvaluations(String keyword) {
        List<NewsComment> comments = newsCommentMapper.selectList(new LambdaQueryWrapper<NewsComment>()
                .orderByDesc(NewsComment::getCreatedAt)
                .orderByDesc(NewsComment::getId));
        Map<Long, String> newsTitleMap = newsMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getId(), item -> item.getTitle(), (left, right) -> left));
        Map<Long, com.civil.platform.module.entity.SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getId(), Function.identity(), (left, right) -> left));
        return comments.stream().map(comment -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", comment.getId());
            item.put("source", "NEWS");
            item.put("resourceType", "NEWS");
            item.put("resourceId", comment.getNewsId());
            item.put("resourceTitle", newsTitleMap.get(comment.getNewsId()));
            item.put("nickname", userMap.get(comment.getUserId()) == null ? "未知用户" : userMap.get(comment.getUserId()).getNickname());
            item.put("rating", null);
            item.put("content", comment.getContent());
            item.put("createdAt", comment.getCreatedAt());
            return item;
        }).filter(item -> !StringUtils.hasText(keyword)
                || containsIgnoreCase(item.get("resourceTitle"), keyword)
                || containsIgnoreCase(item.get("nickname"), keyword)
                || containsIgnoreCase(item.get("content"), keyword))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildOverview(List<CourseOrder> paidOrders,
                                              List<CourseOrderRefund> refunds,
                                              List<CourseComment> courseComments,
                                              List<NewsComment> newsComments,
                                              List<SystemBroadcastMessage> broadcastMessages) {
        Map<String, Object> overview = new HashMap<>();
        overview.put("paidOrderCount", paidOrders.size());
        overview.put("salesAmount", paidOrders.stream().map(CourseOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        overview.put("userCount", sysUserMapper.selectCount(null));
        overview.put("practiceCount", 0L);
        overview.put("refundCount", refunds.size());
        overview.put("courseCommentCount", courseComments.size());
        overview.put("newsCommentCount", newsComments.size());
        overview.put("broadcastCount", broadcastMessages.size());
        overview.put("roleCount", sysRoleMapper.selectCount(null));
        return overview;
    }

    private List<Map<String, Object>> buildSalesTrend(List<CourseOrder> paidOrders) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().toLocalDate().minusDays(i).atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", start.toLocalDate().toString());
            item.put("orderCount", paidOrders.stream().filter(order -> between(order.getPaidAt(), start, end)).count());
            item.put("salesAmount", paidOrders.stream()
                    .filter(order -> between(order.getPaidAt(), start, end))
                    .map(CourseOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildOrderStatusStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(buildStatusStat("PENDING", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "PENDING"))));
        stats.add(buildStatusStat("PAID", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "PAID"))));
        stats.add(buildStatusStat("REFUNDED", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "REFUNDED"))));
        stats.add(buildStatusStat("CANCELLED", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "CANCELLED"))));
        return stats;
    }

    private List<Map<String, Object>> buildRefundStatusStats(List<CourseOrderRefund> refunds) {
        List<String> statuses = Arrays.asList("REQUESTED", "APPROVED", "REFUNDING", "REFUNDED", "REJECTED");
        List<Map<String, Object>> stats = new ArrayList<>();
        for (String status : statuses) {
            stats.add(buildStatusStat(status, refunds.stream().filter(item -> status.equals(item.getRefundStatus())).count()));
        }
        return stats;
    }

    private List<Map<String, Object>> buildSubjectSales(List<CourseOrder> paidOrders) {
        List<Long> orderIds = paidOrders.stream().map(CourseOrder::getId).collect(Collectors.toList());
        List<CourseOrderItem> items = orderIds.isEmpty()
                ? Collections.emptyList()
                : courseOrderItemMapper.selectList(new LambdaQueryWrapper<CourseOrderItem>().in(CourseOrderItem::getOrderId, orderIds));
        Map<Long, String> courseSubjectMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(item -> item.getId(), item -> item.getSubjectName(), (left, right) -> left));
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (CourseOrderItem item : items) {
            String subject = courseSubjectMap.get(item.getCourseId());
            if (!StringUtils.hasText(subject)) {
                subject = "未分类";
            }
            grouped.put(subject, grouped.getOrDefault(subject, BigDecimal.ZERO).add(item.getUnitPrice()));
        }
        return grouped.entrySet().stream().map(entry -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("subjectName", entry.getKey());
            map.put("salesAmount", entry.getValue());
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRoleStats() {
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSortNo)
                .orderByAsc(SysRole::getId));
        Map<Long, Long> roleCountMap = sysUserRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleId, Collectors.counting()));
        return roles.stream().map(role -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("roleId", role.getId());
            item.put("roleCode", role.getCode());
            item.put("roleName", role.getName());
            item.put("userCount", roleCountMap.getOrDefault(role.getId(), 0L));
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCommentStats(List<CourseComment> courseComments, List<NewsComment> newsComments) {
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(buildCommentStat("COURSE", courseComments.size(), averageRating(courseComments)));
        stats.add(buildCommentStat("NEWS", newsComments.size(), BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)));
        return stats;
    }

    private List<Map<String, Object>> buildBroadcastStats(List<SystemBroadcastMessage> messages) {
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(buildStatusStat("广播总数", (long) messages.size()));
        stats.add(buildStatusStat("在线接收数", messages.isEmpty() || messages.get(0).getReceiverCount() == null ? 0L : messages.get(0).getReceiverCount().longValue()));
        return stats;
    }

    private Map<String, Object> buildStatusStat(String name, Long value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }

    private Map<String, Object> buildCommentStat(String resourceType, long count, BigDecimal averageRating) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("resourceType", resourceType);
        map.put("count", count);
        map.put("averageRating", averageRating);
        return map;
    }

    private BigDecimal averageRating(List<CourseComment> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = comments.stream()
                .map(item -> BigDecimal.valueOf(item.getRating() == null ? 0 : item.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(comments.size()), 1, RoundingMode.HALF_UP);
    }

    private int compareDateTime(Object left, Object right) {
        String leftText = left == null ? "" : String.valueOf(left);
        String rightText = right == null ? "" : String.valueOf(right);
        return rightText.compareTo(leftText);
    }

    private boolean between(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private boolean containsIgnoreCase(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> statistics, String key) {
        Object value = statistics.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }
}
