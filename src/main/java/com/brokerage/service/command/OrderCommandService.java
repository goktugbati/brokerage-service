package com.brokerage.service.command;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.mapper.OrderMapper;
import com.brokerage.domain.Customer;
import com.brokerage.domain.Order;
import com.brokerage.domain.OrderStatus;
import com.brokerage.event.*;
import com.brokerage.exception.CustomerNotFoundException;
import com.brokerage.exception.OrderNotFoundException;
import com.brokerage.exception.OrderStatusException;
import com.brokerage.repository.CustomerRepository;
import com.brokerage.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final AssetCommandService assetCommandService;
    private final ResilientEventPublisher eventPublisher;
    private final OrderMapper orderMapper;

    /**
     * Creates a new order with retry mechanism
     *
     * Retry Rationale:
     * - Handle temporary database connection issues
     * - Manage optimistic locking conflicts
     * - Provide resilience for order creation
     */
    @Transactional
    public Order createOrder(Long customerId, CreateOrderRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        Order order = orderMapper.toEntity(request, customer);

        assetCommandService.reserveAssetsForOrder(
                customerId,
                order.getAssetName(),
                order.getOrderSide(),
                order.getSize(),
                order.getPrice()
        );

        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishOrderEvent(OrderCreatedEvent.fromOrder(savedOrder));

        log.info("Created {} order for {} {}, price: {}, customer: {}",
                order.getOrderSide(), order.getSize(), order.getAssetName(), order.getPrice(), customerId);

        return savedOrder;
    }

    /**
     * Fallback method for order creation
     */
    public Order fallbackCreateOrder(Long customerId, CreateOrderRequest request, Exception e) {
        log.error("Failed to create order after retries", e);
        throw new RuntimeException("Unable to create order", e);
    }

    /**
     * Cancels a pending order with retry mechanism
     */
    @Transactional
    public Order cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new OrderStatusException("Only pending orders can be cancelled");
        }

        assetCommandService.releaseReservedAssets(
                customerId,
                order.getAssetName(),
                order.getOrderSide(),
                order.getSize(),
                order.getPrice()
        );

        order.setStatus(OrderStatus.CANCELED);
        Order cancelledOrder = orderRepository.save(order);

        eventPublisher.publishOrderEvent(OrderCancelledEvent.fromOrder(cancelledOrder));

        log.info("Cancelled order {}, customer: {}", orderId, customerId);

        return cancelledOrder;
    }

    /**
     * Fallback method for order cancellation
     */
    public Order fallbackCancelOrder(Long orderId, Long customerId, Exception e) {
        log.error("Failed to cancel order after retries", e);
        throw new RuntimeException("Unable to cancel order", e);
    }

    /**
     * Matches a pending order with circuit breaker
     */
    @Transactional
    public Order matchOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new OrderStatusException("Only pending orders can be matched");
        }

        assetCommandService.updateAssetsForMatchedOrder(
                order.getCustomer().getId(),
                order.getAssetName(),
                order.getOrderSide(),
                order.getSize(),
                order.getPrice()
        );

        order.setStatus(OrderStatus.MATCHED);
        Order matchedOrder = orderRepository.save(order);

        eventPublisher.publishOrderEvent(OrderMatchedEvent.fromOrder(matchedOrder));

        log.info("Matched order {}, customer: {}", orderId, order.getCustomer().getId());

        return matchedOrder;
    }

    /**
     * Fallback method for order matching
     */
    public Order fallbackMatchOrder(Long orderId, Exception e) {
        log.error("Failed to match order due to circuit breaker", e);
        throw new RuntimeException("Unable to match order at this time", e);
    }
}