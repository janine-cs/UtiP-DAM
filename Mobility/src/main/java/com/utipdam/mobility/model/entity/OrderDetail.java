package com.utipdam.mobility.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;

@Entity(name = "order_detail")
@Data
public class OrderDetail {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "total")
    private Double total;

    @Column(name = "created_at")
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "modified_at")
    private Timestamp modifiedAt;


    public OrderDetail() {
    }

    public OrderDetail(Long userId, Integer paymentId, Double total) {
        this.userId = userId;
        this.paymentId = paymentId;
        this.total = total;
    }

}
