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

    @Column(name = "dataset_activation_key")
    private String datasetActivationKey;

    @Column(name = "paypal_order_id")
    private String paypalOrderId;

    @Column(name = "payer_id")
    private String payerId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "payment_source")
    private String paymentSource;

    public enum Currency {
        EUR, USD;
    }
    public PaymentDetail() {
    }

    public PaymentDetail(Integer orderId, Double amount, String description, String status,
                         String paypalOrderId, String payerId, String paymentId, String paymentSource) {
        this.orderId = orderId;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.paypalOrderId = paypalOrderId;
        this.payerId = payerId;
        this.paymentId = paymentId;
        this.paymentSource = paymentSource;
        this.modifiedAt = new Timestamp(System.currentTimeMillis());
    }
}
