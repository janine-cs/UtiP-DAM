package com.utipdam.mobility.model;
import com.utipdam.mobility.model.entity.Organization;
import lombok.Data;

import java.util.UUID;

@Data
public class DatasetResponseDTO {
    private UUID id;
    private String name;
    private String countryCode;
    private String description;
    private Double fee;
    private Boolean publish;
    private Boolean internal;
    private Organization organization;
    private UUID datasetDefinitionId;
    private String resolution;
    private String startDate;
    private String endDate;
    private String updatedOn;
    private Integer k;
    private Long dataPoints;

    public DatasetResponseDTO() {
    }

    public DatasetResponseDTO(UUID id, String name, String description,
                              String countryCode, Double fee, Boolean publish,
                              Boolean internal, Organization organization, UUID datasetDefinitionId,
                              String resolution, String startDate, String endDate,
                              String updatedOn, Integer k, Long dataPoints) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.countryCode = countryCode;
        this.fee = fee;
        this.publish = publish;
        this.internal = internal;
        this.organization = organization;
        this.datasetDefinitionId = datasetDefinitionId;
        this.resolution = resolution;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedOn = updatedOn;
        this.k = k;
        this.dataPoints = dataPoints;
    }
}
