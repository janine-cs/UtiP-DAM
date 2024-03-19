package com.utipdam.internal.model;

import lombok.Data;

import java.util.List;

@Data
public class Token {
    private String token;
    private String type;
    private Integer id;
    private String username;
    private String email;
    private List<String> roles;

    public Token() {
    }
}
