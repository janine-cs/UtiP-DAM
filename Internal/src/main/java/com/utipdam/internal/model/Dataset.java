package com.utipdam.internal.model;

import lombok.Data;

import java.sql.Date;
import java.util.UUID;


@Data
public class Dataset {
    private UUID id;
    private DatasetDefinition datasetDefinition;
    private String resolution;
    private Date startDate;
    private Date endDate;
    private String updatedOn;
    private Integer k;
    private Long dataPoints;


    public Dataset() {

    }
}
