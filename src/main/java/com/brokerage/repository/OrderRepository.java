package com.brokerage.repository;

import com.brokerage.domain.Order;
import com.brokerage.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomerId(Long customerId);
    
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.createDate BETWEEN :startDate AND :endDate")
    List<Order> findByCustomerIdAndDateRange(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.status = :status AND o.createDate BETWEEN :startDate AND :endDate")
    List<Order> findByCustomerIdAndStatusAndDateRange(
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.customer.id = :customerId")
    Optional<Order> findByIdAndCustomerId(
            @Param("orderId") Long orderId,
            @Param("customerId") Long customerId);
    
    List<Order> findByStatus(OrderStatus status);
}
