package com.utipdam.mobility.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Entity(name = "mobility")
@Data
public class Mobility {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "data_points")
    private Long dataPoints;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "updated_on")
    private String updatedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "id", nullable = false)
    private Organization organization;

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
        if (mobility.getDataPoints()!= null) {
            this.dataPoints = mobility.getDataPoints();
        }
        if (mobility.getUpdatedOn()!= null) {
            this.updatedOn = mobility.getUpdatedOn();
        }
    }

}
