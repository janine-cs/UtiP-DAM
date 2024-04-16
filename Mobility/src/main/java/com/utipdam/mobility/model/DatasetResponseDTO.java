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
    private String city;
    private String description;
    private Double fee;
    private Boolean publish;
    private Organization datasetOwner;
    private List<DatasetListDTO> datasets;
    private Long dataPoints;
    private Long userId;
    private String updatedOn;

    public DatasetResponseDTO() {
    }

    public DatasetResponseDTO(String name, String description,
                              String countryCode, String city, Double fee, Boolean publish,
                              Organization organization, UUID datasetDefinitionId,
                              String updatedOn, Long dataPoints, List<DatasetListDTO> datasets, Long userId) {
        this.datasetDefinitionId = datasetDefinitionId;
        this.name = name;
        this.description = description;
        this.countryCode = countryCode;
        this.city = city;
        this.fee = fee;
        this.publish = publish;
        this.datasetOwner = organization;
        this.datasets = datasets;
        this.dataPoints = dataPoints;
        this.userId = userId;
        this.updatedOn = updatedOn;
    }
}
