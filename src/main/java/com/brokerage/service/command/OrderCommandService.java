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
     * Creates a new order using MapStruct for conversion
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
     * Cancels a pending order
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
     * Matches a pending order (admin only)
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
}