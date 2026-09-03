package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionPaymentResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionStatusDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.exception.SubscriptionRequiredException;
import com.two_m.yourbarber.mapper.SubscriptionPaymentMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.SubscriptionPayment;
import com.two_m.yourbarber.model.enums.SubscriptionPaymentStatus;
import com.two_m.yourbarber.model.enums.SubscriptionStatus;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.SubscriptionPaymentRepository;
import com.two_m.yourbarber.service.pix.PixBrCodeGenerator;
import com.two_m.yourbarber.service.pix.PixQrCodeImageGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String TXID_PREFIX = "SUB";
    private static final String DEFAULT_MERCHANT_CITY = "BRASIL";

    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final BarberRepository barberRepository;

    @Value("${subscription.pix-key}")
    private String pixKey;

    @Value("${subscription.merchant-name}")
    private String merchantName;

    @Value("${subscription.amount}")
    private BigDecimal amount;

    @Value("${subscription.period-days}")
    private long periodDays;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusDTO getStatus(Long barberId) {
        return toStatusDto(currentPayments(barberId));
    }

    @Override
    public PixQrCodeResponseDTO requestPixCharge(Long barberId) {
        Barber barber = findBarber(barberId);
        List<SubscriptionPayment> payments =
                subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(barberId);

        SubscriptionPayment payment =
                payments.stream()
                        .filter(p -> p.getStatus() == SubscriptionPaymentStatus.PENDING)
                        .findFirst()
                        .orElseGet(
                                () ->
                                        subscriptionPaymentRepository.save(
                                                SubscriptionPayment.builder()
                                                        .barber(barber)
                                                        .amount(amount)
                                                        .build()));

        if (payment.getTxId() == null) {
            payment.setTxId(TXID_PREFIX + payment.getId());
            payment = subscriptionPaymentRepository.save(payment);
        }

        if (pixKey == null || pixKey.isBlank()) {
            throw new BusinessRuleException("Subscription Pix key is not configured");
        }

        String pixCopyPaste =
                PixBrCodeGenerator.generate(
                        pixKey, merchantName, DEFAULT_MERCHANT_CITY, payment.getAmount(), payment.getTxId());
        String qrCodeBase64 = PixQrCodeImageGenerator.toPngBase64(pixCopyPaste);

        return PixQrCodeResponseDTO.builder()
                .appointmentId(null)
                .pixKey(pixKey)
                .amount(payment.getAmount())
                .merchantName(merchantName)
                .merchantCity(DEFAULT_MERCHANT_CITY)
                .txId(payment.getTxId())
                .pixCopyPaste(pixCopyPaste)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertActive(Long barberId) {
        if (toStatusDto(currentPayments(barberId)).getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionRequiredException(
                    "Sua assinatura está inativa. Realize o pagamento para continuar.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPaymentResponseDTO> listPendingPayments() {
        return subscriptionPaymentRepository
                .findByStatus(SubscriptionPaymentStatus.PENDING)
                .stream()
                .map(SubscriptionPaymentMapper::toDto)
                .toList();
    }

    @Override
    public SubscriptionPaymentResponseDTO decidePayment(Long paymentId, boolean approved) {
        SubscriptionPayment payment =
                subscriptionPaymentRepository
                        .findById(paymentId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Subscription payment not found: " + paymentId));

        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            throw new BusinessRuleException("This payment has already been reviewed");
        }

        payment.setConfirmedAt(LocalDateTime.now());
        if (approved) {
            payment.setStatus(SubscriptionPaymentStatus.CONFIRMED);
            payment.setPeriodStart(LocalDate.now());
            payment.setPeriodEnd(LocalDate.now().plusDays(periodDays));
        } else {
            payment.setStatus(SubscriptionPaymentStatus.REJECTED);
        }

        return SubscriptionPaymentMapper.toDto(subscriptionPaymentRepository.save(payment));
    }

    private List<SubscriptionPayment> currentPayments(Long barberId) {
        return subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(barberId);
    }

    private SubscriptionStatusDTO toStatusDto(List<SubscriptionPayment> payments) {
        LocalDate today = LocalDate.now();

        Optional<SubscriptionPayment> activeConfirmed =
                payments.stream()
                        .filter(p -> p.getStatus() == SubscriptionPaymentStatus.CONFIRMED)
                        .filter(p -> p.getPeriodEnd() != null && !p.getPeriodEnd().isBefore(today))
                        .findFirst();
        if (activeConfirmed.isPresent()) {
            return SubscriptionStatusDTO.builder()
                    .status(SubscriptionStatus.ACTIVE)
                    .periodEnd(activeConfirmed.get().getPeriodEnd())
                    .build();
        }

        boolean hasPending =
                payments.stream().anyMatch(p -> p.getStatus() == SubscriptionPaymentStatus.PENDING);
        if (hasPending) {
            return SubscriptionStatusDTO.builder()
                    .status(SubscriptionStatus.PENDING_CONFIRMATION)
                    .build();
        }

        return SubscriptionStatusDTO.builder().status(SubscriptionStatus.INACTIVE).build();
    }

    private Barber findBarber(Long id) {
        return barberRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found: " + id));
    }
}
