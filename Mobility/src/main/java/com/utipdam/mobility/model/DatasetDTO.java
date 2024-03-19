package com.utipdam.mobility.model;

import lombok.Data;

import java.util.UUID;

@Data
public class DatasetDTO {
    private UUID datasetDefinitionId;
    private String startDate;
    private String endDate;
    private String resolution;
    private Integer k;
    private Long dataPoints;

    public DatasetDTO() {
    }

}
