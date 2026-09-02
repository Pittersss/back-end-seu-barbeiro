package com.two_m.yourbarber.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("BARBER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Barber extends User {

    private String pixKey;

    @Builder.Default
    private boolean available = true;

    private int delayTolerance;

    /** Daily working window, shared across every weekday. Hours are 0-24, wall clock. */
    @Builder.Default private int workStartHour = 9;

    @Builder.Default private int workEndHour = 18;

    /**
     * Optional daily break (e.g. lunch). Both null or both set; when set no appointment
     * may overlap {@code [breakStartHour, breakEndHour)}.
     */
    private Integer breakStartHour;

    private Integer breakEndHour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_shop_id")
    private BarberShop barberShop;
}
