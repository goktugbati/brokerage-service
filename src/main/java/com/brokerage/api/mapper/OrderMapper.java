package com.brokerage.api.mapper;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.domain.Customer;
import com.brokerage.domain.Order;
import com.brokerage.domain.OrderStatus;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {OrderStatus.class, LocalDateTime.class})
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "status", expression = "java(OrderStatus.PENDING)")
    @Mapping(target = "createDate", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updateDate", ignore = true)
    Order toEntity(CreateOrderRequest request, Customer customer);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "assetName", source = "assetName")
    @Mapping(target = "orderSide", source = "orderSide")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createDate", source = "createDate")
    @Mapping(target = "updateDate", source = "updateDate")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);
}