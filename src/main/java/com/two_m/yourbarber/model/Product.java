package com.two_m.yourbarber.model;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class Product {
    @Id
    @Getter
    private long id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private byte[] image;

    @Getter
    @Setter
    private BigDecimal value;

    @Getter
    @Setter
    private String description;
}
