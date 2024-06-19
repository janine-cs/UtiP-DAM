package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findAll();
    Optional<OrderItem> findById(@Param("id") Integer id);
    List<OrderItem> findAllByOrderId(@Param("orderId") Integer orderId);
}
