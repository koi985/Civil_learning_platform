package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.CartAddRequest;
import com.civil.platform.module.dto.OrderCreateRequest;
import com.civil.platform.module.dto.OrderPayRequest;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.CourseOrder;
import com.civil.platform.module.entity.CourseOrderItem;
import com.civil.platform.module.entity.PaymentLog;
import com.civil.platform.module.entity.ShoppingCartItem;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.entity.UserCourseStudyProgress;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.CourseOrderItemMapper;
import com.civil.platform.module.mapper.CourseOrderMapper;
import com.civil.platform.module.mapper.PaymentLogMapper;
import com.civil.platform.module.mapper.ShoppingCartItemMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import com.civil.platform.module.mapper.UserCourseStudyProgressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final ShoppingCartItemMapper shoppingCartItemMapper;
    private final CourseMapper courseMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final CourseOrderItemMapper courseOrderItemMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserCourseStudyProgressMapper userCourseStudyProgressMapper;
    private final RecommendEngineService recommendEngineService;
    private final OrderRefundService orderRefundService;
    private final CourseLearnCountService courseLearnCountService;

    public OrderService(ShoppingCartItemMapper shoppingCartItemMapper,
                        CourseMapper courseMapper,
                        CourseOrderMapper courseOrderMapper,
                        CourseOrderItemMapper courseOrderItemMapper,
                        PaymentLogMapper paymentLogMapper,
                        UserCourseMapper userCourseMapper,
                        UserCourseStudyProgressMapper userCourseStudyProgressMapper,
                        RecommendEngineService recommendEngineService,
                        OrderRefundService orderRefundService,
                        CourseLearnCountService courseLearnCountService) {
        this.shoppingCartItemMapper = shoppingCartItemMapper;
        this.courseMapper = courseMapper;
        this.courseOrderMapper = courseOrderMapper;
        this.courseOrderItemMapper = courseOrderItemMapper;
        this.paymentLogMapper = paymentLogMapper;
        this.userCourseMapper = userCourseMapper;
        this.userCourseStudyProgressMapper = userCourseStudyProgressMapper;
        this.recommendEngineService = recommendEngineService;
        this.orderRefundService = orderRefundService;
        this.courseLearnCountService = courseLearnCountService;
    }

    public List<Map<String, Object>> cartItems() {
        Long userId = SecurityUtils.getUserId();
        List<ShoppingCartItem> items = shoppingCartItemMapper.selectList(new LambdaQueryWrapper<ShoppingCartItem>()
                .eq(ShoppingCartItem::getUserId, userId)
                .orderByDesc(ShoppingCartItem::getCreatedAt));
        return buildCartItems(items);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addCartItem(CartAddRequest request) {
        Long userId = SecurityUtils.getUserId();
        Course course = getPublishedCourse(request.getCourseId());
        if (hasCourseAccess(userId, course.getId())) {
            throw new BizException("该课程已在我的课程中");
        }
        ShoppingCartItem exists = shoppingCartItemMapper.selectOne(new LambdaQueryWrapper<ShoppingCartItem>()
                .eq(ShoppingCartItem::getUserId, userId)
                .eq(ShoppingCartItem::getCourseId, course.getId()));
        if (exists != null) {
            exists.setSelected(true);
            shoppingCartItemMapper.updateById(exists);
            return;
        }
        ShoppingCartItem item = new ShoppingCartItem();
        item.setUserId(userId);
        item.setCourseId(course.getId());
        item.setSelected(true);
        shoppingCartItemMapper.insert(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeCartItem(Long cartItemId) {
        ShoppingCartItem item = getOwnedCartItem(cartItemId);
        shoppingCartItemMapper.deleteById(item.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createDirectOrder(OrderCreateRequest request) {
        if (request.getCourseId() == null) {
            throw new BizException("请选择课程");
        }
        Course course = getPublishedCourse(request.getCourseId());
        Long userId = SecurityUtils.getUserId();
        if (hasCourseAccess(userId, course.getId())) {
            throw new BizException("该课程已购买");
        }
        CourseOrder order = createOrder(userId, request.getPaymentChannel(), request.getRemark(), Collections.singletonList(course));
        return orderDetail(order.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrderFromCart(OrderCreateRequest request) {
        if (CollectionUtils.isEmpty(request.getCartItemIds())) {
            throw new BizException("请选择购物车课程");
        }
        Long userId = SecurityUtils.getUserId();
        List<ShoppingCartItem> items = shoppingCartItemMapper.selectBatchIds(request.getCartItemIds()).stream()
                .filter(item -> userId.equals(item.getUserId()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException("购物车课程不存在");
        }
        List<Course> courses = courseMapper.selectBatchIds(items.stream().map(ShoppingCartItem::getCourseId).collect(Collectors.toList()));
        CourseOrder order = createOrder(userId, request.getPaymentChannel(), request.getRemark(), courses);
        shoppingCartItemMapper.deleteBatchIds(items.stream().map(ShoppingCartItem::getId).collect(Collectors.toList()));
        return orderDetail(order.getId());
    }

    public PageResult<Map<String, Object>> pageOrders(Long pageNo, Long pageSize, String orderStatus) {
        Long userId = SecurityUtils.getUserId();
        Page<CourseOrder> page = courseOrderMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(StringUtils.hasText(orderStatus), CourseOrder::getOrderStatus, orderStatus)
                .orderByDesc(CourseOrder::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(order -> buildOrderSummary(order, loadOrderItems(order.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public Map<String, Object> orderDetail(Long orderId) {
        CourseOrder order = getOwnedOrder(orderId);
        Map<String, Object> result = buildOrderSummary(order, loadOrderItems(order.getId()));
        result.put("paymentLogs", paymentLogMapper.selectList(new LambdaQueryWrapper<PaymentLog>()
                .eq(PaymentLog::getOrderId, orderId)
                .orderByDesc(PaymentLog::getCreatedAt)));
        result.put("refund", orderRefundService.refundByOrderId(orderId));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payOrder(Long orderId, OrderPayRequest request) {
        Long userId = SecurityUtils.getUserId();
        CourseOrder order = getOwnedOrder(orderId);
        if ("PAID".equals(order.getOrderStatus())) {
            return orderDetail(orderId);
        }
        List<CourseOrderItem> items = loadOrderItems(order.getId());
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException("订单缺少课程明细");
        }

        order.setOrderStatus("PAID");
        order.setPaymentStatus("SUCCESS");
        order.setPaymentChannel(request.getPaymentChannel());
        order.setPayAmount(order.getTotalAmount());
        order.setPaidAt(LocalDateTime.now());
        courseOrderMapper.updateById(order);

        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setOrderId(order.getId());
        paymentLog.setUserId(userId);
        paymentLog.setPaymentChannel(request.getPaymentChannel());
        paymentLog.setPaymentAmount(order.getTotalAmount());
        paymentLog.setPaymentStatus("SUCCESS");
        paymentLog.setTransactionNo("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        paymentLog.setPaidAt(LocalDateTime.now());
        paymentLogMapper.insert(paymentLog);

        for (CourseOrderItem item : items) {
            if (!hasCourseAccess(userId, item.getCourseId())) {
                Course course = courseMapper.selectById(item.getCourseId());
                UserCourse userCourse = new UserCourse();
                userCourse.setUserId(userId);
                userCourse.setCourseId(item.getCourseId());
                userCourse.setSourceOrderId(order.getId());
                userCourse.setExpireAt(course == null || course.getValidityDays() == null
                        ? null
                        : LocalDateTime.now().plusDays(course.getValidityDays()));
                userCourseMapper.insert(userCourse);
                courseLearnCountService.syncCourse(item.getCourseId());
            }
        }
        recommendEngineService.enqueueBehaviorRefresh(userId, "COURSE_PURCHASE");

        return orderDetail(orderId);
    }

    public List<Map<String, Object>> myCourses() {
        Long userId = SecurityUtils.getUserId();
        List<UserCourse> userCourses = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .orderByDesc(UserCourse::getCreatedAt));
        List<Course> courses = courseMapper.selectBatchIds(userCourses.stream().map(UserCourse::getCourseId).collect(Collectors.toList()));
        Map<Long, Course> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, item -> item));
        Map<Long, UserCourseStudyProgress> progressMap = userCourseStudyProgressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>()
                        .eq(UserCourseStudyProgress::getUserId, userId)
                        .in(!userCourses.isEmpty(), UserCourseStudyProgress::getCourseId, userCourses.stream().map(UserCourse::getCourseId).collect(Collectors.toList())))
                .stream()
                .collect(Collectors.toMap(UserCourseStudyProgress::getCourseId, item -> item, (left, right) -> left));
        return userCourses.stream()
                .map(item -> {
                    Course course = courseMap.get(item.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    UserCourseStudyProgress progress = progressMap.get(course.getId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("courseId", course.getId());
                    map.put("title", course.getTitle());
                    map.put("coverUrl", course.getCoverUrl());
                    map.put("teacherName", course.getTeacherName());
                    map.put("subjectName", course.getSubjectName());
                    map.put("examType", course.getExamType());
                    map.put("price", course.getPrice());
                    map.put("freeCourse", course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0);
                    map.put("expireAt", item.getExpireAt());
                    map.put("sourceOrderId", item.getSourceOrderId());
                    map.put("progressPercent", progress == null ? BigDecimal.ZERO : progress.getProgressPercent());
                    map.put("completedChapterCount", progress == null ? 0 : progress.getCompletedChapterCount());
                    map.put("totalStudySeconds", progress == null ? 0 : progress.getTotalStudySeconds());
                    map.put("lastStudyAt", progress == null ? null : progress.getLastStudyAt());
                    return map;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    public boolean hasCourseAccess(Long userId, Long courseId) {
        return userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .eq(UserCourse::getCourseId, courseId)) > 0;
    }

    public boolean inCart(Long userId, Long courseId) {
        return shoppingCartItemMapper.selectCount(new LambdaQueryWrapper<ShoppingCartItem>()
                .eq(ShoppingCartItem::getUserId, userId)
                .eq(ShoppingCartItem::getCourseId, courseId)) > 0;
    }

    private List<Map<String, Object>> buildCartItems(List<ShoppingCartItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return new ArrayList<>();
        }
        List<Course> courses = courseMapper.selectBatchIds(items.stream().map(ShoppingCartItem::getCourseId).collect(Collectors.toList()));
        Map<Long, Course> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, item -> item));
        return items.stream()
                .map(item -> {
                    Course course = courseMap.get(item.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("courseId", course.getId());
                    map.put("title", course.getTitle());
                    map.put("coverUrl", course.getCoverUrl());
                    map.put("teacherName", course.getTeacherName());
                    map.put("subjectName", course.getSubjectName());
                    map.put("examType", course.getExamType());
                    map.put("price", course.getPrice());
                    map.put("originalPrice", course.getOriginalPrice());
                    map.put("selected", item.getSelected());
                    map.put("createdAt", item.getCreatedAt());
                    return map;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    private CourseOrder createOrder(Long userId, String paymentChannel, String remark, List<Course> courses) {
        if (CollectionUtils.isEmpty(courses)) {
            throw new BizException("订单中没有课程");
        }
        List<Course> orderCourses = courses.stream()
                .filter(course -> !hasCourseAccess(userId, course.getId()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(orderCourses)) {
            throw new BizException("所选课程已全部购买");
        }
        BigDecimal totalAmount = orderCourses.stream()
                .map(Course::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CourseOrder order = new CourseOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(BigDecimal.ZERO);
        order.setItemCount(orderCourses.size());
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setPaymentChannel(paymentChannel);
        order.setRemark(remark);
        courseOrderMapper.insert(order);

        for (Course course : orderCourses) {
            CourseOrderItem item = new CourseOrderItem();
            item.setOrderId(order.getId());
            item.setCourseId(course.getId());
            item.setCourseTitle(course.getTitle());
            item.setCourseCoverUrl(course.getCoverUrl());
            item.setUnitPrice(course.getPrice());
            item.setQuantity(1);
            courseOrderItemMapper.insert(item);
        }

        return order;
    }

    private List<CourseOrderItem> loadOrderItems(Long orderId) {
        return courseOrderItemMapper.selectList(new LambdaQueryWrapper<CourseOrderItem>()
                .eq(CourseOrderItem::getOrderId, orderId)
                .orderByAsc(CourseOrderItem::getId));
    }

    private Map<String, Object> buildOrderSummary(CourseOrder order, List<CourseOrderItem> items) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("totalAmount", order.getTotalAmount());
        map.put("payAmount", order.getPayAmount());
        map.put("itemCount", order.getItemCount());
        map.put("orderStatus", order.getOrderStatus());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("paymentChannel", order.getPaymentChannel());
        map.put("remark", order.getRemark());
        map.put("paidAt", order.getPaidAt());
        map.put("createdAt", order.getCreatedAt());
        map.put("items", items);
        map.put("refund", orderRefundService.refundSummary(order.getId()));
        return map;
    }

    private Course getPublishedCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !"PUBLISHED".equals(course.getStatus())) {
            throw new BizException("课程不存在");
        }
        return course;
    }

    private ShoppingCartItem getOwnedCartItem(Long cartItemId) {
        ShoppingCartItem item = shoppingCartItemMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException("购物车记录不存在");
        }
        return item;
    }

    private CourseOrder getOwnedOrder(Long orderId) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
    }
}
