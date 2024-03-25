package com.utipdam.internal.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Server {
    private UUID id;

    private String name;

    private String domain;


    public Server() {
    }

}
