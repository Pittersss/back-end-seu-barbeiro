package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.notification.NotificationResponseDTO;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.enums.NotificationType;
import java.util.List;

public interface NotificationService {

    List<NotificationResponseDTO> listForUser(Long userId);

    long countUnread(Long userId);

    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);

    /** Persists an in-app notification for {@code recipientId} and pushes it to their devices. */
    void notify(Long recipientId, NotificationType type, String message, Appointment appointment);
}
