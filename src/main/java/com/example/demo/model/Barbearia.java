package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
    @OneToOne
    @JoinColumn(name = "id")
    private Barbeiro dono;

    @Getter
    @Setter
    private String endereco;
}
