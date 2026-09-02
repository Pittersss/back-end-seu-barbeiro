package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.user.UpdateProfileDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userProfileService.getMe(currentUser.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMe(
            @Valid @RequestBody UpdateProfileDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userProfileService.updateMe(currentUser.getId(), dto));
    }
}
