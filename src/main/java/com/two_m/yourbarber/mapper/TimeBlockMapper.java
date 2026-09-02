package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.timeblock.TimeBlockResponseDTO;
import com.two_m.yourbarber.model.TimeBlock;

public final class TimeBlockMapper {

    private TimeBlockMapper() {}

    public static TimeBlockResponseDTO toDto(TimeBlock block) {
        return TimeBlockResponseDTO.builder()
                .id(block.getId())
                .startsAt(block.getStartsAt())
                .endsAt(block.getEndsAt())
                .reason(block.getReason())
                .barberId(block.getBarber().getId())
                .build();
    }
}
