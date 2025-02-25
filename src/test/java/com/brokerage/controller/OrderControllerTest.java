package com.brokerage.controller;

import com.brokerage.api.CustomerHelper;
import com.brokerage.api.OrderController;
import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.mapper.OrderMapper;
import com.brokerage.domain.Customer;
import com.brokerage.domain.Order;
import com.brokerage.domain.OrderSide;
import com.brokerage.domain.OrderStatus;
import com.brokerage.security.SecurityUser;
import com.brokerage.service.command.OrderCommandService;
import com.brokerage.service.query.OrderQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderCommandService orderCommandService;

    @MockBean
    private OrderQueryService orderQueryService;

    @MockBean
    private OrderMapper orderMapper;

    @MockBean
    private CustomerHelper customerHelper;

    private Order testOrder;
    private OrderResponse orderResponse;
    private CreateOrderRequest createOrderRequest;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        // Set up test customer
        Customer testCustomer = Customer.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .isAdmin(false)
                .build();

        // Set up test order
        testOrder = Order.builder()
                .id(1L)
                .customer(testCustomer)
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        // Set up order response
        orderResponse = OrderResponse.builder()
                .id(1L)
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        // Set up create order request
        createOrderRequest = CreateOrderRequest.builder()
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .build();

        // Set up security user
        securityUser = new SecurityUser(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                1L
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_ShouldCreateOrderSuccessfully() throws Exception {
        // Arrange
        when(customerHelper.getCustomerIdFromUserDetails(any())).thenReturn(1L);
        when(orderCommandService.createOrder(anyLong(), any(CreateOrderRequest.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Order created successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.assetName", is("TRY")))
                .andExpect(jsonPath("$.data.orderSide", is("BUY")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        verify(orderCommandService).createOrder(eq(1L), any(CreateOrderRequest.class));
        verify(orderMapper).toResponse(eq(testOrder));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getOrders_ShouldReturnOrderList() throws Exception {
        // Arrange
        when(customerHelper.getCustomerIdFromUserDetails(any())).thenReturn(1L);
        List<Order> orders = Arrays.asList(testOrder);
        List<OrderResponse> orderResponses = Arrays.asList(orderResponse);
        
        when(orderQueryService.getOrdersByCustomerId(anyLong())).thenReturn(orders);
        when(orderMapper.toResponseList(anyList())).thenReturn(orderResponses);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Orders retrieved successfully")))
                .andExpect(jsonPath("$.data.orders", hasSize(1)))
                .andExpect(jsonPath("$.data.count", is(1)))
                .andExpect(jsonPath("$.data.orders[0].id", is(1)))
                .andExpect(jsonPath("$.data.orders[0].assetName", is("TRY")))
                .andExpect(jsonPath("$.data.orders[0].orderSide", is("BUY")));

        verify(orderQueryService).getOrdersByCustomerId(eq(1L));
        verify(orderMapper).toResponseList(eq(orders));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getOrder_ShouldReturnOrderById() throws Exception {
        // Arrange
        when(customerHelper.getCustomerIdFromUserDetails(any())).thenReturn(1L);
        when(orderQueryService.getOrderByIdAndCustomerId(anyLong(), anyLong())).thenReturn(testOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders/1")
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Order retrieved successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.assetName", is("TRY")))
                .andExpect(jsonPath("$.data.orderSide", is("BUY")));

        verify(orderQueryService).getOrderByIdAndCustomerId(eq(1L), eq(1L));
        verify(orderMapper).toResponse(eq(testOrder));
    }

    @Test
    @WithMockUser(roles = "USER")
    void cancelOrder_ShouldCancelOrderSuccessfully() throws Exception {
        // Arrange
        when(customerHelper.getCustomerIdFromUserDetails(any())).thenReturn(1L);
        Order cancelledOrder = testOrder;
        cancelledOrder.setStatus(OrderStatus.CANCELED);
        
        OrderResponse cancelledResponse = orderResponse;
        cancelledResponse.setStatus(OrderStatus.CANCELED);
        
        when(orderCommandService.cancelOrder(anyLong(), anyLong())).thenReturn(cancelledOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(cancelledResponse);

        // Act & Assert
        mockMvc.perform(delete("/api/orders/1")
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Order cancelled successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.status", is("CANCELED")));

        verify(orderCommandService).cancelOrder(eq(1L), eq(1L));
        verify(orderMapper).toResponse(eq(cancelledOrder));
    }

    @Test
    void getOrders_WithoutAuthentication_ShouldReturn401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        verify(orderQueryService, never()).getOrdersByCustomerId(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrdersWithFilters_ShouldReturnFilteredOrders() throws Exception {
        // Arrange
        when(customerHelper.getCustomerIdFromUserDetails(any())).thenReturn(1L);
        List<Order> orders = Arrays.asList(testOrder);
        List<OrderResponse> orderResponses = Arrays.asList(orderResponse);
        
        when(orderQueryService.getOrdersByCustomerIdAndStatus(anyLong(), any(OrderStatus.class)))
                .thenReturn(orders);
        when(orderMapper.toResponseList(anyList())).thenReturn(orderResponses);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders", hasSize(1)))
                .andExpect(jsonPath("$.data.orders[0].status", is("PENDING")));

        verify(orderQueryService).getOrdersByCustomerIdAndStatus(eq(1L), eq(OrderStatus.PENDING));
    }
}