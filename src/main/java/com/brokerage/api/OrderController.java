package com.brokerage.api;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.response.ApiResponse;
import com.brokerage.api.dto.response.OrderListResponse;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.mapper.OrderMapper;
import com.brokerage.domain.Order;
import com.brokerage.domain.OrderStatus;
import com.brokerage.service.command.OrderCommandService;
import com.brokerage.service.query.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final OrderMapper orderMapper;
    private final CustomerHelper customerHelper;

    @PostMapping
    @Operation(summary = "Create order", description = "Create a new pending order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long customerId = customerHelper.getCustomerIdFromUserDetails(userDetails);
        log.info("Creating order for customer ID: {}", customerId);

        Order order = orderCommandService.createOrder(customerId, request);
        OrderResponse response = orderMapper.toResponse(order);

        return ResponseEntity.ok(new ApiResponse<>(true, "Order created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List orders", description = "List orders with optional filters")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long customerId = customerHelper.getCustomerIdFromUserDetails(userDetails);
        log.info("Fetching orders for customer ID: {}", customerId);

        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status) : null;

        List<Order> orders;
        if (startDate != null && endDate != null) {
            if (orderStatus != null) {
                orders = orderQueryService.getOrdersByCustomerIdAndStatusAndDateRange(
                        customerId, orderStatus, startDate, endDate);
            } else {
                orders = orderQueryService.getOrdersByCustomerIdAndDateRange(
                        customerId, startDate, endDate);
            }
        } else if (orderStatus != null) {
            orders = orderQueryService.getOrdersByCustomerIdAndStatus(customerId, orderStatus);
        } else {
            orders = orderQueryService.getOrdersByCustomerId(customerId);
        }

        List<OrderResponse> orderResponses = orderMapper.toResponseList(orders);

        OrderListResponse response = OrderListResponse.builder()
                .orders(orderResponses)
                .count(orderResponses.size())
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Orders retrieved successfully", response));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Get order by ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long customerId = customerHelper.getCustomerIdFromUserDetails(userDetails);
        log.info("Fetching order ID: {} for customer ID: {}", orderId, customerId);

        Order order = orderQueryService.getOrderByIdAndCustomerId(orderId, customerId);
        OrderResponse response = orderMapper.toResponse(order);

        return ResponseEntity.ok(new ApiResponse<>(true, "Order retrieved successfully", response));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order", description = "Cancel a pending order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long customerId = customerHelper.getCustomerIdFromUserDetails(userDetails);
        log.info("Cancelling order ID: {} for customer ID: {}", orderId, customerId);

        Order order = orderCommandService.cancelOrder(orderId, customerId);
        OrderResponse response = orderMapper.toResponse(order);

        return ResponseEntity.ok(new ApiResponse<>(true, "Order cancelled successfully", response));
    }
}