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

    public DatasetDefinition() {
    }

}
