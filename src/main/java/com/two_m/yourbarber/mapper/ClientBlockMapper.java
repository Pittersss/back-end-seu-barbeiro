package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.clientblock.ClientBlockResponseDTO;
import com.two_m.yourbarber.model.ClientBlock;

public final class ClientBlockMapper {

    private ClientBlockMapper() {}

    public static ClientBlockResponseDTO toDto(ClientBlock block) {
        return ClientBlockResponseDTO.builder()
                .id(block.getId())
                .clientId(block.getClient().getId())
                .clientName(block.getClient().getName())
                .clientPhone(block.getClient().getPhone())
                .reason(block.getReason())
                .createdAt(block.getCreatedAt())
                .build();
    }
}
