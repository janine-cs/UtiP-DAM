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
    private Integer monthLicense;
    private String paypalOrderID;
    private String payerID;
    private String paymentID;
    private String paymentSource;

    public OrderDTO() {
    }

}
