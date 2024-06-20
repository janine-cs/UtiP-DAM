package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findAll();
    Optional<OrderItem> findById(@Param("id") Integer id);
    List<OrderItem> findAllByOrderId(@Param("orderId") Integer orderId);
    @Query(nativeQuery = true, value = "SELECT i.* FROM order_item i INNER JOIN order_detail o ON i.order_id = o.id WHERE o.user_id = :userId")
    List<OrderItem> findAllByUserId(@Param("userId") Long userId);

}
