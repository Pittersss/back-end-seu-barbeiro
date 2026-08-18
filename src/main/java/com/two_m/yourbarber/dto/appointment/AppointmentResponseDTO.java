package com.two_m.yourbarber.dto.appointment;

import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.model.enums.PaymentMethod;
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
public class AppointmentResponseDTO {

    private Long id;
    private LocalDateTime scheduledAt;
    private AppointmentStatus status;
    private PaymentMethod paymentMethod;
    private Long clientId;
    private String clientName;
    private Long barberId;
    private String barberName;
    private Long serviceId;
    private String serviceName;
}
