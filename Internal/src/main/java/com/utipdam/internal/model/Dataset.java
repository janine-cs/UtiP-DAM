package com.utipdam.internal.model;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Data
public class Dataset {
    private UUID id;
    private UUID datasetDefinitionId;
    private String resolution;
    private String startDate;
    private String endDate;
    private String updatedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    private Integer k;
    private Long dataPoints;


    public Dataset() {
    }

}
