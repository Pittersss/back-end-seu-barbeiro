package com.two_m.yourbarber.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;
import org.springframework.data.annotation.Id;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberShop {
    @Id
    @Getter
    private long id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    @OneToOne
    @JoinColumn(name = "id")
    private Barber owner;

    @Getter
    @Setter
    private String address;
}
