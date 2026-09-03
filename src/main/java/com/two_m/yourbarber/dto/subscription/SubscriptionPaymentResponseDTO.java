package com.two_m.yourbarber.dto.subscription;

import com.two_m.yourbarber.model.enums.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class SubscriptionPaymentResponseDTO {

    private Long id;
    private Long barberId;
    private String barberName;
    private SubscriptionPaymentStatus status;
    private BigDecimal amount;
    private String txId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
