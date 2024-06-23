package com.utipdam.mobility.model;

import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

@Data
public class PurchaseDTO {
    private Integer purchaseId;
    private UUID datasetDefinitionId;
    private String datasetName;
    private String datasetDescription;
    private String status;
    private String paymentStatus;
    private UUID datasetActivationKey;
    private Date licenseStartDate;
    private Date licenseEndDate;
    private String datasetURL;
    private Date purchaseDate;
    private Double purchasePrice;
    private String currency;
    private String datasetPublisher;
    private Timestamp createdAt;
    private Timestamp modifiedOn;

    public PurchaseDTO() {
    }

    public PurchaseDTO(Integer purchaseId, UUID datasetDefinitionId, String datasetName, String datasetDescription,
                       String status, String paymentStatus, UUID apiKey,
                       Date licenseStartDate, Date licenseEndDate, String datasetURL, Date purchaseDate,
                       Double purchasePrice, String currency, String datasetPublisher, Timestamp createdAt, Timestamp modifiedOn) {
        this.purchaseId = purchaseId;
        this.datasetDefinitionId = datasetDefinitionId;
        this.datasetName = datasetName;
        this.datasetDescription = datasetDescription;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.datasetActivationKey = apiKey;
        this.licenseStartDate = licenseStartDate;
        this.licenseEndDate = licenseEndDate;
        this.datasetURL = datasetURL;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.currency = currency;
        this.datasetPublisher = datasetPublisher;
        this.createdAt = createdAt;
        this.modifiedOn = modifiedOn;
    }
}
