package com.two_m.yourbarber.model;

import com.two_m.yourbarber.model.abstract_entities.User;
import jakarta.persistence.Id;
import lombok.Getter;

public class Client extends User {
    @Id
    @Getter
    private long id;
}
