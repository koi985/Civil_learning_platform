package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.CartAddRequest;
import com.civil.platform.module.dto.OrderCreateRequest;
import com.civil.platform.module.dto.OrderPayRequest;
import com.civil.platform.module.dto.UserRefundRequest;
import com.civil.platform.module.service.OrderService;
import com.civil.platform.module.service.OrderRefundService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final OrderRefundService orderRefundService;

    public OrderController(OrderService orderService, OrderRefundService orderRefundService) {
        this.orderService = orderService;
        this.orderRefundService = orderRefundService;
    }

    @GetMapping("/cart/items")
    public ApiResponse<List<Map<String, Object>>> cartItems() {
        return ApiResponse.success(orderService.cartItems());
    }

    @PostMapping("/cart/items")
    @OperationLog(module = "订单支付", name = "加入购物车")
    public ApiResponse<Void> addCartItem(@RequestBody @Valid CartAddRequest request) {
        orderService.addCartItem(request);
        return ApiResponse.success("加入成功", null);
    }

    @DeleteMapping("/cart/items/{cartItemId}")
    @OperationLog(module = "订单支付", name = "移出购物车")
    public ApiResponse<Void> removeCartItem(@PathVariable Long cartItemId) {
        orderService.removeCartItem(cartItemId);
        return ApiResponse.success("移除成功", null);
    }

    @PostMapping("/orders/direct")
    @OperationLog(module = "订单支付", name = "创建直购订单")
    public ApiResponse<Map<String, Object>> createDirectOrder(@RequestBody @Valid OrderCreateRequest request) {
        return ApiResponse.success(orderService.createDirectOrder(request));
    }

    @PostMapping("/orders/from-cart")
    @OperationLog(module = "订单支付", name = "创建购物车订单")
    public ApiResponse<Map<String, Object>> createOrderFromCart(@RequestBody @Valid OrderCreateRequest request) {
        return ApiResponse.success(orderService.createOrderFromCart(request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<Map<String, Object>>> orders(@RequestParam(defaultValue = "1") Long pageNo,
                                                               @RequestParam(defaultValue = "10") Long pageSize,
                                                               @RequestParam(required = false) String orderStatus) {
        return ApiResponse.success(orderService.pageOrders(pageNo, pageSize, orderStatus));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> orderDetail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.orderDetail(orderId));
    }

    @PostMapping("/orders/{orderId}/pay")
    @OperationLog(module = "订单支付", name = "模拟支付")
    public ApiResponse<Map<String, Object>> payOrder(@PathVariable Long orderId,
                                                     @RequestBody @Valid OrderPayRequest request) {
        return ApiResponse.success(orderService.payOrder(orderId, request));
    }

    @PostMapping("/orders/{orderId}/refund")
    @OperationLog(module = "订单支付", name = "申请退款")
    public ApiResponse<Map<String, Object>> requestRefund(@PathVariable Long orderId,
                                                          @RequestBody @Valid UserRefundRequest request) {
        return ApiResponse.success(orderRefundService.requestRefund(orderId, request));
    }
}
