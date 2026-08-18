package com.two_m.yourbarber.dto.appointment;

import com.two_m.yourbarber.model.enums.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentPostDTO {

    @NotNull private Long barberId;

    @NotNull private Long serviceId;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;

    @NotNull private PaymentMethod paymentMethod;
}
