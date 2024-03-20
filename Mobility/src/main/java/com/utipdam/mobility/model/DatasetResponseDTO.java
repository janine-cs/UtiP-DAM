package com.utipdam.mobility.model;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.Organization;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DatasetResponseDTO {

    private UUID datasetDefinitionId;
    private String name;
    private String countryCode;
    private String description;
    private Double fee;
    private Boolean publish;
    private Organization datasetOwner;
    private List<DatasetListDTO> datasets;
    private Long avgDataPoints;
    private String updatedOn;

    public DatasetResponseDTO() {
    }

    public DatasetResponseDTO(String name, String description,
                              String countryCode, Double fee, Boolean publish,
                              Organization organization, UUID datasetDefinitionId,
                              String updatedOn, Long avgDataPoints, List<DatasetListDTO> datasets) {
        this.datasetDefinitionId = datasetDefinitionId;
        this.name = name;
        this.description = description;
        this.countryCode = countryCode;
        this.fee = fee;
        this.publish = publish;
        this.datasetOwner = organization;
        this.datasets = datasets;
        this.avgDataPoints = avgDataPoints;
        this.updatedOn = updatedOn;
    }
}
