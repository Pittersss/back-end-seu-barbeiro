package com.example.demo.model;

import jakarta.persistence.Entity;
import lombok.*;
import org.springframework.data.annotation.Id;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Barbearia {
    @Id
    @Getter
    private long id;

    @Getter
    @Setter
    private String nome;

    @Getter
    @Setter
    private Barbeiro dono;

    @Getter
    @Setter
    private String endereco;
}
