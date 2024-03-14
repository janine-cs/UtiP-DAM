package com.utipdam.mobility.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.UUID;

@Entity(name = "organization")
@Data
public class Organization {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;


    public Organization() {
    }

    public Organization(UUID id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }


    public void update(Organization organization) {
        if (organization.getName() != null) {
            this.name = organization.getName();
        }
        if (organization.getEmail() != null) {
            this.name = organization.getEmail();
        }
    }

}
