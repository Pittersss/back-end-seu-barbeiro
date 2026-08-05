package com.two_m.yourbarber.model;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;

public class Service {
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
    private Duration duration;

    @Getter
    @Setter
    private BigDecimal value;
}
