package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionPaymentResponseDTO;
import com.two_m.yourbarber.dto.subscription.SubscriptionStatusDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.SubscriptionRequiredException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.SubscriptionPayment;
import com.two_m.yourbarber.model.enums.SubscriptionPaymentStatus;
import com.two_m.yourbarber.model.enums.SubscriptionStatus;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.SubscriptionPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private SubscriptionPaymentRepository subscriptionPaymentRepository;
    @Mock private BarberRepository barberRepository;

    @InjectMocks private SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "pixKey", "dev@example.com");
        ReflectionTestUtils.setField(subscriptionService, "merchantName", "Seu Barbeiro");
        ReflectionTestUtils.setField(subscriptionService, "amount", new BigDecimal("30.00"));
        ReflectionTestUtils.setField(subscriptionService, "periodDays", 30L);
    }

    private Barber barber(long id) {
        Barber barber =
                Barber.builder()
                        .name("Barber")
                        .email("barber@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .build();
        barber.setId(id);
        return barber;
    }

    private SubscriptionPayment payment(
            long id, Barber barber, SubscriptionPaymentStatus status, LocalDate periodEnd) {
        SubscriptionPayment payment =
                SubscriptionPayment.builder()
                        .barber(barber)
                        .status(status)
                        .amount(new BigDecimal("30.00"))
                        .periodEnd(periodEnd)
                        .build();
        payment.setId(id);
        return payment;
    }

    @Test
    void getStatus_noPayments_returnsInactive() {
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        SubscriptionStatusDTO result = subscriptionService.getStatus(1L);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.INACTIVE);
    }

    @Test
    void getStatus_pendingPayment_returnsPendingConfirmation() {
        Barber barber = barber(1L);
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(payment(10L, barber, SubscriptionPaymentStatus.PENDING, null)));

        SubscriptionStatusDTO result = subscriptionService.getStatus(1L);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING_CONFIRMATION);
    }

    @Test
    void getStatus_confirmedWithFuturePeriodEnd_returnsActive() {
        Barber barber = barber(1L);
        LocalDate periodEnd = LocalDate.now().plusDays(10);
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(
                        List.of(
                                payment(
                                        10L,
                                        barber,
                                        SubscriptionPaymentStatus.CONFIRMED,
                                        periodEnd)));

        SubscriptionStatusDTO result = subscriptionService.getStatus(1L);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getPeriodEnd()).isEqualTo(periodEnd);
    }

    @Test
    void getStatus_confirmedWithPastPeriodEnd_returnsInactive() {
        Barber barber = barber(1L);
        LocalDate periodEnd = LocalDate.now().minusDays(1);
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(
                        List.of(
                                payment(
                                        10L,
                                        barber,
                                        SubscriptionPaymentStatus.CONFIRMED,
                                        periodEnd)));

        SubscriptionStatusDTO result = subscriptionService.getStatus(1L);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.INACTIVE);
    }

    @Test
    void assertActive_inactive_throws() {
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        assertThrows(
                SubscriptionRequiredException.class, () -> subscriptionService.assertActive(1L));
    }

    @Test
    void assertActive_active_doesNotThrow() {
        Barber barber = barber(1L);
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(
                        List.of(
                                payment(
                                        10L,
                                        barber,
                                        SubscriptionPaymentStatus.CONFIRMED,
                                        LocalDate.now().plusDays(5))));

        subscriptionService.assertActive(1L);
    }

    @Test
    void requestPixCharge_noPendingPayment_createsOne() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());
        when(subscriptionPaymentRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            SubscriptionPayment p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(42L);
                            }
                            return p;
                        });

        PixQrCodeResponseDTO result = subscriptionService.requestPixCharge(1L);

        assertThat(result.getTxId()).isEqualTo("SUB42");
        assertThat(result.getAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getPixKey()).isEqualTo("dev@example.com");
        assertThat(result.getQrCodeBase64()).isNotBlank();
    }

    @Test
    void requestPixCharge_existingPendingPayment_reusesIt() {
        Barber barber = barber(1L);
        SubscriptionPayment pending = payment(7L, barber, SubscriptionPaymentStatus.PENDING, null);
        pending.setTxId("SUB7");
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));
        when(subscriptionPaymentRepository.findByBarberIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(pending));

        PixQrCodeResponseDTO result = subscriptionService.requestPixCharge(1L);

        assertThat(result.getTxId()).isEqualTo("SUB7");
        verify(subscriptionPaymentRepository, never()).save(any());
    }

    @Test
    void decidePayment_approved_setsConfirmedWithPeriod() {
        Barber barber = barber(1L);
        SubscriptionPayment pending = payment(7L, barber, SubscriptionPaymentStatus.PENDING, null);
        when(subscriptionPaymentRepository.findById(7L)).thenReturn(Optional.of(pending));
        when(subscriptionPaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionPaymentResponseDTO result = subscriptionService.decidePayment(7L, true);

        assertThat(result.getStatus()).isEqualTo(SubscriptionPaymentStatus.CONFIRMED);
        assertThat(result.getPeriodStart()).isEqualTo(LocalDate.now());
        assertThat(result.getPeriodEnd()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void decidePayment_rejected_setsRejectedWithoutPeriod() {
        Barber barber = barber(1L);
        SubscriptionPayment pending = payment(7L, barber, SubscriptionPaymentStatus.PENDING, null);
        when(subscriptionPaymentRepository.findById(7L)).thenReturn(Optional.of(pending));
        when(subscriptionPaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionPaymentResponseDTO result = subscriptionService.decidePayment(7L, false);

        assertThat(result.getStatus()).isEqualTo(SubscriptionPaymentStatus.REJECTED);
        assertThat(result.getPeriodEnd()).isNull();
    }

    @Test
    void decidePayment_alreadyReviewed_throws() {
        Barber barber = barber(1L);
        SubscriptionPayment confirmed =
                payment(7L, barber, SubscriptionPaymentStatus.CONFIRMED, LocalDate.now().plusDays(30));
        when(subscriptionPaymentRepository.findById(7L)).thenReturn(Optional.of(confirmed));

        assertThrows(
                BusinessRuleException.class, () -> subscriptionService.decidePayment(7L, true));
    }

    @Test
    void listPendingPayments_returnsOnlyPending() {
        Barber barber = barber(1L);
        when(subscriptionPaymentRepository.findByStatus(SubscriptionPaymentStatus.PENDING))
                .thenReturn(List.of(payment(7L, barber, SubscriptionPaymentStatus.PENDING, null)));

        List<SubscriptionPaymentResponseDTO> result = subscriptionService.listPendingPayments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBarberId()).isEqualTo(1L);
    }
}
