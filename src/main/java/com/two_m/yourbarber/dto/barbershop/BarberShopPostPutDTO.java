package com.two_m.yourbarber.dto.barbershop;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BarberShopPostPutDTO {

    @NotBlank private String name;

    private String address;

    private String phone;

    private String photoBase64;
}
