package com.two_m.yourbarber.model;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

public class Activity {
    @Id
    private long id;

    @Getter
    @Setter
    @OneToMany(mappedBy = "id")
    private Service service;

    @Getter
    @Setter
    @OneToMany(mappedBy = "id")
    private Barber barber;

    @Getter
    @Setter
    private Date date;

    @Getter
    @Setter
    private Date start;

    @Getter
    @Setter
    private Date end;
}
