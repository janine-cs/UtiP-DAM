package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Entity(name = "mobility")
@Data
public class Mobility {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "dataset_id")
    private UUID datasetId;

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


    public Mobility() {
    }

    public void update(Mobility mobility) {
        if (mobility.getResolution() != null) {
            this.resolution = mobility.getResolution();
        }
        if (mobility.getStartDate()!= null) {
            this.startDate = mobility.getStartDate();
        }
        if (mobility.getEndDate()!= null) {
            this.endDate = mobility.getEndDate();
        }
        if (mobility.getUpdatedOn()!= null) {
            this.updatedOn = mobility.getUpdatedOn();
        }
    }

}
