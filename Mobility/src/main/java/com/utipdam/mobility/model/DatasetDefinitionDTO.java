package com.utipdam.mobility.model;
import com.utipdam.mobility.model.entity.Organization;
import lombok.Data;

@Data
public class DatasetDefinitionDTO {
    private String name;
    private String description;
    private String countryCode;
    private String city;
    private Double fee;
    private Organization organization;
    private boolean publish = false;
    private boolean internal = false;
    private Integer k;
    private String resolution = "daily";
    private Long userId;

    public DatasetDefinitionDTO() {
    }

}
