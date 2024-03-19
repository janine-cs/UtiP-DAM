package com.utipdam.internal.model;

import lombok.Data;

import java.util.UUID;


@Data
public class Dataset {
    private UUID id;
    private DatasetDefinition datasetDefinition;
    private String resolution;
    private String startDate;
    private String endDate;
    private String updatedOn;
    private Integer k;
    private Long dataPoints;


    public Dataset() {

    }
}
