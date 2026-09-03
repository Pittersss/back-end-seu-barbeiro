package com.two_m.yourbarber.dto.barbershop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberShopResponseDTO {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String photoBase64;
    private boolean acceptingBarbers;
    private Long ownerId;
    private String ownerName;
}
