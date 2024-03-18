package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Entity(name = "dataset")
@Data
public class Dataset {
    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dataset_definition_id")
    private DatasetDefinition datasetDefinition;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "updated_on")
    private String updatedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

    @Column(name = "k_value")
    private Integer kValue;

    @Column(name = "data_points")
    private Long dataPoints;


    public Dataset() {
    }

    public void update(Dataset dataset) {
        if (dataset.getResolution() != null) {
            this.resolution = dataset.getResolution();
        }
        if (dataset.getStartDate()!= null) {
            this.startDate = dataset.getStartDate();
        }
        if (dataset.getEndDate()!= null) {
            this.endDate = dataset.getEndDate();
        }
        if (dataset.getUpdatedOn()!= null) {
            this.updatedOn = dataset.getUpdatedOn();
        }
    }

}
