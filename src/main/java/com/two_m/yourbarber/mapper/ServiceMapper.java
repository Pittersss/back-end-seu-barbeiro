package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.service.ServiceResponseDTO;

public final class ServiceMapper {

    private ServiceMapper() {}

    public static ServiceResponseDTO toDto(com.two_m.yourbarber.model.Service service) {
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .available(service.isAvailable())
                .barberShopId(
                        service.getBarberShop() != null
                                ? service.getBarberShop().getId()
                                : null)
                .barberId(service.getBarber() != null ? service.getBarber().getId() : null)
                .barberName(service.getBarber() != null ? service.getBarber().getName() : null)
                .build();
    }
}
