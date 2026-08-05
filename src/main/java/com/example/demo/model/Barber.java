package com.example.demo.model;

import com.example.demo.model.abstract_entities.User;
import jakarta.persistence.Entity;
import lombok.*;
import org.springframework.data.annotation.Id;

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
    private int delayTolerance;
}
