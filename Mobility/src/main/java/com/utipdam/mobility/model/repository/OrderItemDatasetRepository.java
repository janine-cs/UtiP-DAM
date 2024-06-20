package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.OrderItemDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemDatasetRepository extends JpaRepository<OrderItemDataset, Integer> {
    List<OrderItemDataset> findAll();
    Optional<OrderItemDataset> findById(@Param("id") Integer id);
    List<OrderItemDataset> findAllByOrderItemId(@Param("orderItemId") Integer orderItemId);

}
