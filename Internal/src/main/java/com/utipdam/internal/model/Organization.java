package com.utipdam.internal.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Organization {
    private UUID id;
    private String name;
    private String email;

    public Organization() {
    }

}
