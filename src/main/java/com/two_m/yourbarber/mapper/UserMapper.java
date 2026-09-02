package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.user.UserProfileDTO;
import com.two_m.yourbarber.model.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserProfileDTO toProfileDto(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .avatarBase64(user.getAvatarBase64())
                .build();
    }
}
