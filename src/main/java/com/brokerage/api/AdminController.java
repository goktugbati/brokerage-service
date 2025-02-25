package com.brokerage.api;

import com.brokerage.api.dto.request.CreateCustomerRequest;
import com.brokerage.api.dto.request.MatchOrderRequest;
import com.brokerage.api.dto.response.ApiResponse;
import com.brokerage.api.dto.response.CustomerResponse;
import com.brokerage.api.dto.response.OrderListResponse;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.mapper.CustomerMapper;
import com.brokerage.api.mapper.OrderMapper;
import com.brokerage.domain.Customer;
import com.brokerage.domain.Order;
import com.brokerage.service.CustomerService;
import com.brokerage.service.command.OrderCommandService;
import com.brokerage.service.query.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin operations API")
public class AdminController {

    private final CustomerService customerService;
    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;

    @GetMapping("/customers")
    @Operation(summary = "List customers", description = "List all customers (admin only)")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        log.info("Admin fetching all customers");

        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponse> customerResponses = customerMapper.toResponseList(customers);

        return ResponseEntity.ok(new ApiResponse<>(true, "Customers retrieved successfully", customerResponses));
    }

    @PostMapping("/customers")
    @Operation(summary = "Create customer", description = "Create a new customer (admin only)")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        log.info("Admin creating new customer: {}", request.getUsername());

        Customer customer = customerService.createCustomer(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFullName(),
                false
        );

        CustomerResponse response = customerMapper.toResponse(customer);

        return ResponseEntity.ok(new ApiResponse<>(true, "Customer created successfully", response));
    }

    @PostMapping("/customers/admin")
    @Operation(summary = "Create admin", description = "Create a new admin user (admin only)")
    public ResponseEntity<ApiResponse<CustomerResponse>> createAdmin(@Valid @RequestBody CreateCustomerRequest request) {
        log.info("Admin creating new admin user: {}", request.getUsername());

        Customer admin = customerService.createCustomer(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFullName(),
                true
        );

        CustomerResponse response = customerMapper.toResponse(admin);

        return ResponseEntity.ok(new ApiResponse<>(true, "Admin created successfully", response));
    }

    @GetMapping("/orders/pending")
    @Operation(summary = "List pending orders", description = "List all pending orders (admin only)")
    public ResponseEntity<ApiResponse<OrderListResponse>> getPendingOrders() {
        log.info("Admin fetching all pending orders");

        List<Order> pendingOrders = orderQueryService.getAllPendingOrders();
        List<OrderResponse> orderResponses = orderMapper.toResponseList(pendingOrders);

        OrderListResponse response = OrderListResponse.builder()
                .orders(orderResponses)
                .count(orderResponses.size())
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Pending orders retrieved successfully", response));
    }

    @PostMapping("/orders/match")
    @Operation(summary = "Match order", description = "Match a pending order (admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> matchOrder(@Valid @RequestBody MatchOrderRequest request) {
        log.info("Admin matching order ID: {}", request.getOrderId());

        Order matchedOrder = orderCommandService.matchOrder(request.getOrderId());
        OrderResponse response = orderMapper.toResponse(matchedOrder);

        return ResponseEntity.ok(new ApiResponse<>(true, "Order matched successfully", response));
    }
}