package com.utipdam.mobility.model;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderDTO {
    private Long userId;
    private UUID datasetId;

    public OrderDTO() {
    }

}
