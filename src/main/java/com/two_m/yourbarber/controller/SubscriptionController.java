package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionStatusDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionStatusDTO> getStatus(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(subscriptionService.getStatus(currentUser.getId()));
    }

    @PostMapping("/me/pix")
    public ResponseEntity<PixQrCodeResponseDTO> requestPixCharge(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(subscriptionService.requestPixCharge(currentUser.getId()));
    }
}
