package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.subscription.SubscriptionPaymentResponseDTO;
import com.two_m.yourbarber.model.SubscriptionPayment;

public final class SubscriptionPaymentMapper {

    private SubscriptionPaymentMapper() {}

    public static SubscriptionPaymentResponseDTO toDto(SubscriptionPayment payment) {
        return SubscriptionPaymentResponseDTO.builder()
                .id(payment.getId())
                .barberId(payment.getBarber() != null ? payment.getBarber().getId() : null)
                .barberName(payment.getBarber() != null ? payment.getBarber().getName() : null)
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .txId(payment.getTxId())
                .periodStart(payment.getPeriodStart())
                .periodEnd(payment.getPeriodEnd())
                .createdAt(payment.getCreatedAt())
                .confirmedAt(payment.getConfirmedAt())
                .build();
    }
}
