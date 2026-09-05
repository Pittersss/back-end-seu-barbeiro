package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.notification.NotificationResponseDTO;
import com.two_m.yourbarber.model.Notification;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationResponseDTO toDto(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .appointmentId(
                        notification.getAppointment() != null
                                ? notification.getAppointment().getId()
                                : null)
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
