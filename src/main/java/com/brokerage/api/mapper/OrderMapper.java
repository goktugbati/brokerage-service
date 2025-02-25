package com.brokerage.api.mapper;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.request.OrderFilterRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.domain.Customer;
import com.brokerage.domain.Order;
import com.brokerage.domain.OrderStatus;
import com.brokerage.exception.InvalidOrderException;
import org.mapstruct.*;

import java.math.BigDecimal;
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
    
    /**
     * Validate the order filter request
     * @param request The order filter request to validate
     * @return The same request, potentially with normalized values
     */
    @BeforeMapping
    default OrderFilterRequest validateOrderFilter(OrderFilterRequest request) {

        if (request.getStartDate() != null || request.getEndDate() != null) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new InvalidOrderException("Both startDate and endDate must be provided for date filtering");
            }
            
            if (!request.getStartDate().isBefore(request.getEndDate())) {
                throw new InvalidOrderException("startDate must be before endDate");
            }
        }
        
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                OrderStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new InvalidOrderException("Invalid order status: " + request.getStatus());
            }
        }
        
        return request;
    }

    @BeforeMapping
    default void validateTryAssetRule(CreateOrderRequest request, @MappingTarget Order order) {
        if (!"TRY".equalsIgnoreCase(request.getAssetName())) {
            throw new InvalidOrderException("Only TRY asset is allowed for orders. Other assets are not supported.");
        }

        if (request.getSize().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Order size must be greater than zero");
        }

        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Order price must be greater than zero");
        }
    }
}