package com.brokerage.event;

import com.brokerage.domain.Order;
import com.brokerage.domain.OrderSide;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderMatchedEvent extends BaseEvent {
    private final Long orderId;
    private final Long customerId;
    private final String assetName;
    private final OrderSide orderSide;
    private final BigDecimal size;
    private final BigDecimal price;
    private final BigDecimal totalValue;

    @Builder
    public OrderMatchedEvent(Long orderId, Long customerId, String assetName, 
                             OrderSide orderSide, BigDecimal size, BigDecimal price,
                             BigDecimal totalValue) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.assetName = assetName;
        this.orderSide = orderSide;
        this.size = size;
        this.price = price;
        this.totalValue = totalValue;
    }

    public static OrderMatchedEvent fromOrder(Order order) {
        return OrderMatchedEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomer().getId())
                .assetName(order.getAssetName())
                .orderSide(order.getOrderSide())
                .size(order.getSize())
                .price(order.getPrice())
                .totalValue(order.getSize().multiply(order.getPrice()))
                .build();
    }

    @Override
    public String getEventType() {
        return "ORDER_MATCHED";
    }
}
