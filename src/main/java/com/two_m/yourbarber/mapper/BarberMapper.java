package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.model.Barber;

public final class BarberMapper {

    private BarberMapper() {}

    public static BarberResponseDTO toDto(Barber barber) {
        return BarberResponseDTO.builder()
                .id(barber.getId())
                .name(barber.getName())
                .email(barber.getEmail())
                .phone(barber.getPhone())
                .pixKey(barber.getPixKey())
                .available(barber.isAvailable())
                .delayTolerance(barber.getDelayTolerance())
                .barberShopId(
                        barber.getBarberShop() != null ? barber.getBarberShop().getId() : null)
                .build();
    }
}
