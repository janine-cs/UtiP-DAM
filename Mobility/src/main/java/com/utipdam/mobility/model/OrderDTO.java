package com.utipdam.mobility.model;

import lombok.Data;

import java.sql.Date;
import java.util.UUID;

@Data
public class OrderDTO {
    private Long userId;
    private UUID datasetDefinitionId;
    private Date startDate;
    private Date endDate;
    private String description;
    private boolean oneDay = false;
    private boolean pastDate = false;
    private boolean futureDate = false;
    private String currency;
    private Integer monthLicense;
    private String paymentId;
    private String payerId;
    private String paymentSource;

    public OrderDTO() {
    }

}
