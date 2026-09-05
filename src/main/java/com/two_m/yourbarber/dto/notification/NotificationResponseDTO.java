package com.two_m.yourbarber.dto.notification;

import com.two_m.yourbarber.model.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    private Long id;
    private NotificationType type;
    private String message;
    private Long appointmentId;
    private boolean read;
    private LocalDateTime createdAt;
}
