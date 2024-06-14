package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "order_item")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_definition_id")
    private UUID datasetDefinitionId;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "one_day")
    private boolean oneDay;

    @Column(name = "past_date")
    private boolean pastDate;

    @Column(name = "future_date")
    private boolean futureDate;

    @Column(name = "month_license")
    private Integer monthLicense;

    @Column(name = "created_at")
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "modified_at")
    private Timestamp modifiedAt;


    public OrderItem() {
    }

    public OrderItem(UUID datasetDefinitionId, Integer orderId, Date startDate, Date endDate, boolean oneDay,
                     boolean pastDate, boolean futureDate, Integer monthLicense) {
        this.datasetDefinitionId = datasetDefinitionId;
        this.orderId = orderId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.oneDay = oneDay;
        this.pastDate = pastDate;
        this.futureDate = futureDate;
        this.monthLicense = monthLicense;
        this.modifiedAt = new Timestamp(System.currentTimeMillis());
    }

}
