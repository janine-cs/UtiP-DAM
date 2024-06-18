package com.utipdam.internal.model;

import lombok.Data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private Set<Role> roles = new HashSet<>();
    private Boolean active;
    private Timestamp endDate;
    private Vendor vendor;


    public User() {
    }

}
