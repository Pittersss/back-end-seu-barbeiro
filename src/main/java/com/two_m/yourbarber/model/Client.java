package com.two_m.yourbarber.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("CLIENT")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Client extends User {
    // Client-specific fields would go here. `name`, `phone`, `avatarBase64` and
    // credentials all live on User.
}
