package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "order_item")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private UUID datasetId;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "created_at")
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "modified_at")
    private Timestamp modifiedAt;


    public OrderItem() {
    }

    public OrderItem(UUID datasetId, Integer orderId) {
        this.datasetId = datasetId;
        this.orderId = orderId;
    }

}
