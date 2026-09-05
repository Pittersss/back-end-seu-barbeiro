package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.notification.NotificationResponseDTO;
import com.two_m.yourbarber.dto.notification.UnreadCountResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> listNotifications(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationService.listForUser(currentUser.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDTO> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                UnreadCountResponseDTO.builder()
                        .count(notificationService.countUnread(currentUser.getId()))
                        .build());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        notificationService.markRead(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
