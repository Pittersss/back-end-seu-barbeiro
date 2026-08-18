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

    private String phone;

    private String pixKey;

    @Builder.Default
    private boolean available = true;

    private int delayTolerance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_shop_id")
    private BarberShop barberShop;
}
