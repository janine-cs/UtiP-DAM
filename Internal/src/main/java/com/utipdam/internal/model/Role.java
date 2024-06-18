package com.utipdam.internal.model;

import lombok.Data;

@Data
public class Role {
    private Integer id;
    private ERole name;

    public Role() {
    }

}
