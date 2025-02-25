package com.brokerage.service.command;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.mapper.OrderMapper;
import com.brokerage.domain.Customer;
import com.brokerage.domain.Order;
import com.brokerage.domain.OrderSide;
import com.brokerage.domain.OrderStatus;
import com.brokerage.event.OrderCancelledEvent;
import com.brokerage.event.OrderCreatedEvent;
import com.brokerage.event.OrderMatchedEvent;
import com.brokerage.event.ResilientEventPublisher;
import com.brokerage.exception.CustomerNotFoundException;
import com.brokerage.exception.OrderNotFoundException;
import com.brokerage.exception.OrderStatusException;
import com.brokerage.repository.CustomerRepository;
import com.brokerage.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AssetCommandService assetCommandService;

    @Mock
    private ResilientEventPublisher eventPublisher;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderCommandService orderCommandService;

    private Customer testCustomer;
    private Order testOrder;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        // Set up test customer
        testCustomer = Customer.builder()
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

        // Set up create order request
        createOrderRequest = CreateOrderRequest.builder()
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
        when(orderMapper.toEntity(any(CreateOrderRequest.class), any(Customer.class))).thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(assetCommandService).reserveAssetsForOrder(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
        doNothing().when(eventPublisher).publishOrderEvent(any(OrderCreatedEvent.class));

        // Act
        Order result = orderCommandService.createOrder(1L, createOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(OrderSide.BUY, result.getOrderSide());
        verify(assetCommandService).reserveAssetsForOrder(
                eq(1L), eq("TRY"), eq(OrderSide.BUY), eq(BigDecimal.valueOf(10)), eq(BigDecimal.valueOf(100)));
        verify(eventPublisher).publishOrderEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_WhenCustomerNotFound_ShouldThrowException() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            orderCommandService.createOrder(1L, createOrderRequest);
        });

        verify(orderRepository, never()).save(any(Order.class));
        verify(assetCommandService, never()).reserveAssetsForOrder(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void cancelOrder_ShouldCancelPendingOrderSuccessfully() {
        // Arrange
        Order pendingOrder = Order.builder()
                .id(1L)
                .customer(testCustomer)
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(orderRepository.findByIdAndCustomerId(anyLong(), anyLong())).thenReturn(Optional.of(pendingOrder));

        Order cancelledOrder = Order.builder()
                .id(1L)
                .customer(testCustomer)
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .status(OrderStatus.CANCELED)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);
        doNothing().when(assetCommandService).releaseReservedAssets(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
        doNothing().when(eventPublisher).publishOrderEvent(any(OrderCancelledEvent.class));

        // Act
        Order result = orderCommandService.cancelOrder(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELED, result.getStatus());
        verify(assetCommandService).releaseReservedAssets(
                eq(1L), eq("TRY"), eq(OrderSide.BUY), eq(BigDecimal.valueOf(10)), eq(BigDecimal.valueOf(100)));
        verify(eventPublisher).publishOrderEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void cancelOrder_WhenOrderNotFound_ShouldThrowException() {
        // Arrange
        when(orderRepository.findByIdAndCustomerId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderCommandService.cancelOrder(1L, 1L);
        });

        verify(assetCommandService, never()).releaseReservedAssets(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void cancelOrder_WhenOrderNotPending_ShouldThrowException() {
        // Arrange
        testOrder.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findByIdAndCustomerId(anyLong(), anyLong())).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(OrderStatusException.class, () -> {
            orderCommandService.cancelOrder(1L, 1L);
        });

        verify(assetCommandService, never()).releaseReservedAssets(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void matchOrder_ShouldMatchPendingOrderSuccessfully() {
        // Arrange
        Order pendingOrder = Order.builder()
                .id(1L)
                .customer(testCustomer)
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(pendingOrder));

        Order matchedOrder = Order.builder()
                .id(1L)
                .customer(testCustomer)
                .assetName("TRY")
                .orderSide(OrderSide.BUY)
                .size(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(100))
                .status(OrderStatus.MATCHED)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(matchedOrder);
        doNothing().when(assetCommandService).updateAssetsForMatchedOrder(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
        doNothing().when(eventPublisher).publishOrderEvent(any(OrderMatchedEvent.class));

        // Act
        Order result = orderCommandService.matchOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.MATCHED, result.getStatus());

        // Capture and verify the Order object passed to the save method
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(OrderStatus.MATCHED, orderCaptor.getValue().getStatus());

        verify(assetCommandService).updateAssetsForMatchedOrder(
                eq(1L), eq("TRY"), eq(OrderSide.BUY), eq(BigDecimal.valueOf(10)), eq(BigDecimal.valueOf(100)));
        verify(eventPublisher).publishOrderEvent(any(OrderMatchedEvent.class));
    }

    @Test
    void matchOrder_WhenOrderNotFound_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderCommandService.matchOrder(1L);
        });

        verify(assetCommandService, never()).updateAssetsForMatchedOrder(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void matchOrder_WhenOrderNotPending_ShouldThrowException() {
        // Arrange
        testOrder.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(OrderStatusException.class, () -> {
            orderCommandService.matchOrder(1L);
        });

        verify(assetCommandService, never()).updateAssetsForMatchedOrder(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }
}