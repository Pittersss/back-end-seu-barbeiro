package com.example.demo.model.abstract_entities;

import lombok.Getter;
import lombok.Setter;

public abstract class User {
    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String email;

    @Getter
    @Setter
    private String password;
}
