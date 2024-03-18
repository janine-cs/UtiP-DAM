package com.utipdam.mobility.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Entity(name = "dataset_definition")
@Data
public class DatasetDefinition implements Serializable {
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


    public DatasetDefinition() {
    }

    public void update(DatasetDefinition datasetDefinition) {
        if (datasetDefinition.getName() != null) {
            this.name = datasetDefinition.getName();
        }
        if (datasetDefinition.getDescription()!= null) {
            this.description = datasetDefinition.getDescription();
        }
        if (datasetDefinition.getFee()!= null) {
            this.fee = datasetDefinition.getFee();
        }
        if (datasetDefinition.getOrganization()!= null) {
            this.organization = datasetDefinition.getOrganization();
        }
        if (datasetDefinition.getCountryCode()!= null) {
            this.countryCode = datasetDefinition.getCountryCode();
        }
        if (datasetDefinition.getPublish()!= null) {
            this.publish = datasetDefinition.getPublish();
        }
        if (datasetDefinition.getInternal()!= null) {
            this.internal = datasetDefinition.getInternal();
        }
    }
}
