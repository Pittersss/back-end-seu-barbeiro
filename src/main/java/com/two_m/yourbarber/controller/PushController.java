package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.push.PushSubscriptionRequestDTO;
import com.two_m.yourbarber.dto.push.PushUnsubscribeRequestDTO;
import com.two_m.yourbarber.dto.push.VapidPublicKeyResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.push.PushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<VapidPublicKeyResponseDTO> getVapidPublicKey() {
        return ResponseEntity.ok(
                VapidPublicKeyResponseDTO.builder()
                        .publicKey(pushService.getVapidPublicKey())
                        .build());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody PushSubscriptionRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {
        pushService.subscribe(currentUser.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(@RequestBody PushUnsubscribeRequestDTO dto) {
        pushService.unsubscribe(dto);
        return ResponseEntity.noContent().build();
    }
}
