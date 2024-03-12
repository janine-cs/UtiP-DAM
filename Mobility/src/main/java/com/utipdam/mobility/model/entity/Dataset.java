package com.utipdam.mobility.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Entity(name = "dataset")
@Data
public class Dataset {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "description")
    private String description;

    @Column(name = "fee")
    private Double fee;

    @Column(name = "updated_on")
    private String updatedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

    @Column(name = "organization_id")
    private Integer organizationId;

    public Dataset() {
    }

    public void update(Dataset dataset) {
        if (dataset.getName() != null) {
            this.name = dataset.getName();
        }
        if (dataset.description!= null) {
            this.description = dataset.getDescription();
        }
        if (dataset.fee!= null) {
            this.fee = dataset.getFee();
        }
        if (dataset.organizationId!= null) {
            this.organizationId = dataset.getOrganizationId();
        }
        if (dataset.countryCode!= null) {
            this.countryCode = dataset.getCountryCode();
        }
    }
}
