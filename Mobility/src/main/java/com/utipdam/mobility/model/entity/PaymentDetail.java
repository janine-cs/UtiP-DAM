package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity(name = "payment_detail")
@Data
public class PaymentDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "modified_at")
    private Timestamp modifiedAt;

    @Column(name = "stripe_email")
    private String stripeEmail;

    @Column(name = "currency")
    private Currency currency;

    @Column(name = "balance_transaction")
    private String balanceTransaction;


    public enum Currency {
        EUR, USD;
    }
    public PaymentDetail() {
    }

    public PaymentDetail(Integer orderId, Double amount, String description, String status) {
        this.orderId = orderId;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.modifiedAt = new Timestamp(System.currentTimeMillis());
    }
}
