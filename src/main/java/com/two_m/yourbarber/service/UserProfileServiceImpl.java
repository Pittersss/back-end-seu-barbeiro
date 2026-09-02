package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.user.UpdateProfileDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.UserMapper;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getMe(Long userId) {
        return UserMapper.toProfileDto(findUser(userId));
    }

    @Override
    public UserProfileDTO updateMe(Long userId, UpdateProfileDTO dto) {
        User user = findUser(userId);

        user.setName(dto.getName().trim());
        user.setPhone(blankToNull(dto.getPhone()));
        user.setAvatarBase64(blankToNull(dto.getAvatarBase64()));

        return UserMapper.toProfileDto(userRepository.save(user));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private User findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
