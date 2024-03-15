package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Entity(name = "dataset")
@Data
public class Dataset implements Serializable {
    @Id
    @Column(name = "id")
    private UUID id;

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

//    @Column(name = "organization_id")
//    private UUID organizationId;

    @Column(name = "publish")
    private Boolean publish;

    @Column(name = "internal")
    private Boolean internal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;


    public Dataset() {
    }

    public void update(Dataset dataset) {
        if (dataset.getName() != null) {
            this.name = dataset.getName();
        }
        if (dataset.getDescription()!= null) {
            this.description = dataset.getDescription();
        }
        if (dataset.getFee()!= null) {
            this.fee = dataset.getFee();
        }
        if (dataset.getOrganization()!= null) {
            this.organization = dataset.getOrganization();
        }
        if (dataset.getCountryCode()!= null) {
            this.countryCode = dataset.getCountryCode();
        }
        if (dataset.getPublish()!= null) {
            this.publish = dataset.getPublish();
        }
    }
}
