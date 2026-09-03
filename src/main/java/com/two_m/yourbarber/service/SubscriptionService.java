package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionPaymentResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionStatusDTO;
import java.util.List;

public interface SubscriptionService {

    SubscriptionStatusDTO getStatus(Long barberId);

    PixQrCodeResponseDTO requestPixCharge(Long barberId);

    /** Throws {@link com.two_m.yourbarber.exception.SubscriptionRequiredException} unless ACTIVE. */
    void assertActive(Long barberId);

    List<SubscriptionPaymentResponseDTO> listPendingPayments();

    SubscriptionPaymentResponseDTO decidePayment(Long paymentId, boolean approved);
}
