package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.joinrequest.JoinRequestResponseDTO;
import com.two_m.yourbarber.model.JoinRequest;

public final class JoinRequestMapper {

    private JoinRequestMapper() {}

    public static JoinRequestResponseDTO toDto(JoinRequest request) {
        return JoinRequestResponseDTO.builder()
                .id(request.getId())
                .status(request.getStatus().name())
                .message(request.getMessage())
                .barberId(request.getBarber().getId())
                .barberName(request.getBarber().getName())
                .barberShopId(request.getBarberShop().getId())
                .barberShopName(request.getBarberShop().getName())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
