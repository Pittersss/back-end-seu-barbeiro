package com.two_m.yourbarber.model;

import com.two_m.yourbarber.model.abstract_entities.User;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Barber extends User {
    @Id
    @Getter
    private long id;

    @Getter
    @Setter
    @OneToMany(mappedBy = "id")
    private List<Service> services;

    @Getter
    @Setter
    private int delayTolerance;
}
