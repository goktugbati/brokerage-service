package com.brokerage.service.query;

import com.brokerage.domain.Order;
import com.brokerage.domain.OrderStatus;
import com.brokerage.exception.OrderNotFoundException;
import com.brokerage.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    
    /**
     * Get all orders for a customer
     */
    @Cacheable(value = "customerOrders", key = "#customerId")
    public List<Order> getOrdersByCustomerId(Long customerId) {
        log.debug("Fetching orders for customer ID: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }
    
    /**
     * Get all orders for a customer with status filter
     */
    @Cacheable(value = "customerOrders", key = "#customerId + '-' + #status")
    public List<Order> getOrdersByCustomerIdAndStatus(Long customerId, OrderStatus status) {
        log.debug("Fetching orders for customer ID: {} with status: {}", customerId, status);
        return orderRepository.findByCustomerIdAndStatus(customerId, status);
    }
    
    /**
     * Get all orders for a customer within date range
     */
    @Cacheable(value = "customerOrders", key = "#customerId + '-' + #startDate + '-' + #endDate")
    public List<Order> getOrdersByCustomerIdAndDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching orders for customer ID: {} between {} and {}", customerId, startDate, endDate);
        return orderRepository.findByCustomerIdAndDateRange(customerId, startDate, endDate);
    }
    
    /**
     * Get all orders for a customer with status and date range filters
     */
    @Cacheable(value = "customerOrders", key = "#customerId + '-' + #status + '-' + #startDate + '-' + #endDate")
    public List<Order> getOrdersByCustomerIdAndStatusAndDateRange(
            Long customerId, OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching orders for customer ID: {} with status: {} between {} and {}", 
                customerId, status, startDate, endDate);
        return orderRepository.findByCustomerIdAndStatusAndDateRange(customerId, status, startDate, endDate);
    }
    
    /**
     * Get an order by ID
     */
    @Cacheable(value = "orders", key = "#orderId")
    public Order getOrderById(Long orderId) {
        log.debug("Fetching order by ID: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
    }
    
    /**
     * Get an order by ID and customer ID (for security)
     */
    @Cacheable(value = "orders", key = "#orderId + '-' + #customerId")
    public Order getOrderByIdAndCustomerId(Long orderId, Long customerId) {
        log.debug("Fetching order ID: {} for customer ID: {}", orderId, customerId);
        return orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(
                    String.format("Order %d not found for customer ID %d", orderId, customerId)));
    }
    
    /**
     * Get all pending orders (for admin)
     */
    @Cacheable(value = "orders", key = "'pending'")
    public List<Order> getAllPendingOrders() {
        log.debug("Fetching all pending orders");
        return orderRepository.findByStatus(OrderStatus.PENDING);
    }
}