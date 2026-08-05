package com.two_m.yourbarber.model.abstract_entities;

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
