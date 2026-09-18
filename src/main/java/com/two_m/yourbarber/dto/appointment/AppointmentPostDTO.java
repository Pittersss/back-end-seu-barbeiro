package com.two_m.yourbarber.dto.appointment;

import com.two_m.yourbarber.model.enums.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
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

    /** Legacy single-service field; used only when {@code serviceIds} is empty. */
    private Long serviceId;

    private List<Long> serviceIds;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;

    @NotNull private PaymentMethod paymentMethod;

    public AppointmentPostDTO(
            Long barberId, Long serviceId, LocalDateTime scheduledAt, PaymentMethod paymentMethod) {
        this(barberId, serviceId, null, scheduledAt, paymentMethod);
    }
}
