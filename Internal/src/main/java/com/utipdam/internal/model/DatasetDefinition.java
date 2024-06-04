package com.utipdam.internal.model;

import lombok.Data;

import java.util.UUID;

@Data
public class DatasetDefinition {
    private UUID id;
    private String name;
    private String countryCode;
    private String city;
    private String description;
    private Double fee;
    private String updatedOn;
    private Boolean publish;
    private Boolean internal;
    private Organization organization;
    private Server server;
    private Long userId;
    private Boolean publishMDS;
    private String publishedOn;
    private Double fee1d;
    private Double fee3mo;
    private Double fee6mo;
    private Double fee12mo;

    public DatasetDefinition() {
    }

}
