package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.user.UpdateProfileDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;

public interface UserProfileService {

    UserProfileDTO getMe(Long userId);

    UserProfileDTO updateMe(Long userId, UpdateProfileDTO dto);
}
