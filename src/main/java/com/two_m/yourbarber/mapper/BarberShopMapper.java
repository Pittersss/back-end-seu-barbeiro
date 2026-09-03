package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopResponseDTO;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.BarberShopRequest;

public final class BarberShopMapper {

    private BarberShopMapper() {}

    public static BarberShopResponseDTO toDto(BarberShop shop) {
        return BarberShopResponseDTO.builder()
                .id(shop.getId())
                .name(shop.getName())
                .address(shop.getAddress())
                .phone(shop.getPhone())
                .photoBase64(shop.getPhotoBase64())
                .acceptingBarbers(shop.isAcceptingBarbers())
                .ownerId(shop.getOwner() != null ? shop.getOwner().getId() : null)
                .ownerName(shop.getOwner() != null ? shop.getOwner().getName() : null)
                .build();
    }

    public static BarberShopRequestResponseDTO toRequestDto(BarberShopRequest request) {
        return BarberShopRequestResponseDTO.builder()
                .id(request.getId())
                .status(request.getStatus().name())
                .shopName(request.getShopName())
                .shopAddress(request.getShopAddress())
                .shopPhone(request.getShopPhone())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
