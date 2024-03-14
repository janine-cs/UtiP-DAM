package com.utipdam.mobility.model;
import com.utipdam.mobility.model.entity.Organization;
import lombok.Data;

@Data
public class DatasetDTO {
    private String name;
    private String countryCode;
    private String description;
    private Double fee;
    private Organization organization;
    private Boolean publish;
    private Boolean internal;
    private Integer kValue;
    private String resolution;

    public DatasetDTO() {
    }

}
