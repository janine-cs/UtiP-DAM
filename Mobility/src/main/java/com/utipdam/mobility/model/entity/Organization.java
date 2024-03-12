package com.utipdam.mobility.model.entity;

import lombok.Data;

import javax.persistence.*;

@Entity(name = "organization")
@Data
public class Organization {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;


    public Organization() {
    }

    public Organization(String name, String email) {
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
