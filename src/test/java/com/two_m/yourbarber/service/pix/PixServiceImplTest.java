package com.two_m.yourbarber.service.pix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.pix.PixPreviewDTO;
import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.model.enums.PaymentMethod;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.AppointmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PixServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;

    @InjectMocks private PixServiceImpl pixService;

    private Client client(long id) {
        Client client =
                Client.builder()
                        .name("Client")
                        .email("client@example.com")
                        .password("x")
                        .role(UserRole.CLIENT)
                        .build();
        client.setId(id);
        return client;
    }

    private Barber barber(long id, String pixKey) {
        Barber barber =
                Barber.builder()
                        .name("Barber")
                        .email("barber@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .pixKey(pixKey)
                        .barberShop(BarberShop.builder().name("Shop").build())
                        .build();
        barber.setId(id);
        return barber;
    }

    private com.two_m.yourbarber.model.Service offering(long id, BigDecimal price) {
        com.two_m.yourbarber.model.Service service =
                com.two_m.yourbarber.model.Service.builder()
                        .name("Haircut")
                        .durationMinutes(30)
                        .price(price)
                        .available(true)
                        .build();
        service.setId(id);
        return service;
    }

    private Appointment appointment(
            long id,
            Client client,
            Barber barber,
            com.two_m.yourbarber.model.Service service,
            AppointmentStatus status,
            PaymentMethod paymentMethod) {
        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(LocalDateTime.now().plusDays(1))
                        .status(status)
                        .paymentMethod(paymentMethod)
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();
        appointment.setId(id);
        return appointment;
    }

    @Test
    void generateQrCode_client_returnsPayloadAndQrCodeImage() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.PENDING,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        PixQrCodeResponseDTO result = pixService.generateQrCode(7L, 1L);

        assertThat(result.getAppointmentId()).isEqualTo(7L);
        assertThat(result.getPixKey()).isEqualTo("barbeiro@example.com");
        assertThat(result.getAmount()).isEqualByComparingTo("45.00");
        assertThat(result.getTxId()).isEqualTo("APT7");
        assertThat(result.getPixCopyPaste()).startsWith("000201");
        assertThat(result.getPixCopyPaste()).contains("APT7");
        assertThat(result.getQrCodeBase64()).isNotBlank();
    }

    @Test
    void generateQrCode_barberIsAlsoAllowed_returnsPayload() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.CONFIRMED,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        PixQrCodeResponseDTO result = pixService.generateQrCode(7L, 2L);

        assertThat(result.getPixCopyPaste()).isNotBlank();
    }

    @Test
    void generateQrCode_nonParticipant_throwsForbidden() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.PENDING,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(
                ForbiddenOperationException.class, () -> pixService.generateQrCode(7L, 99L));
    }

    @Test
    void generateQrCode_notPixPaymentMethod_throwsBusinessRule() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.PENDING,
                        PaymentMethod.CASH);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(BusinessRuleException.class, () -> pixService.generateQrCode(7L, 1L));
    }

    @Test
    void generateQrCode_cancelledAppointment_throwsBusinessRule() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.CANCELLED,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(BusinessRuleException.class, () -> pixService.generateQrCode(7L, 1L));
    }

    @Test
    void generateQrCode_completedAppointment_throwsBusinessRule() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.COMPLETED,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(BusinessRuleException.class, () -> pixService.generateQrCode(7L, 1L));
    }

    @Test
    void generateQrCode_barberHasNoPixKey_throwsBusinessRule() {
        Client client = client(1L);
        Barber barber = barber(2L, null);
        com.two_m.yourbarber.model.Service service = offering(3L, new BigDecimal("45.00"));
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.PENDING,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(BusinessRuleException.class, () -> pixService.generateQrCode(7L, 1L));
    }

    @Test
    void generateQrCode_serviceHasNoPrice_throwsBusinessRule() {
        Client client = client(1L);
        Barber barber = barber(2L, "barbeiro@example.com");
        com.two_m.yourbarber.model.Service service = offering(3L, null);
        Appointment appointment =
                appointment(
                        7L,
                        client,
                        barber,
                        service,
                        AppointmentStatus.PENDING,
                        PaymentMethod.PIX);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(BusinessRuleException.class, () -> pixService.generateQrCode(7L, 1L));
    }

    @Test
    void generateQrCode_appointmentNotFound_throwsResourceNotFound() {
        when(appointmentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> pixService.generateQrCode(7L, 1L));
    }

    @Test
    void preview_buildsPayloadAndImageFromRawValues() {
        PixPreviewDTO dto =
                new PixPreviewDTO(
                        "pedromalaquias608@gmail.com",
                        "Seu Barbeiro",
                        new BigDecimal("1.00"),
                        "TESTE1");

        PixQrCodeResponseDTO result = pixService.preview(dto);

        assertThat(result.getAppointmentId()).isNull();
        assertThat(result.getPixKey()).isEqualTo("pedromalaquias608@gmail.com");
        assertThat(result.getAmount()).isEqualByComparingTo("1.00");
        assertThat(result.getTxId()).isEqualTo("TESTE1");
        assertThat(result.getPixCopyPaste()).startsWith("000201");
        assertThat(result.getPixCopyPaste()).contains("pedromalaquias608@gmail.com");
        assertThat(result.getQrCodeBase64()).isNotBlank();
    }

    @Test
    void preview_blankTxId_isTolerated() {
        PixPreviewDTO dto =
                new PixPreviewDTO("11999998888", "Barber", new BigDecimal("10.00"), "  ");

        PixQrCodeResponseDTO result = pixService.preview(dto);

        assertThat(result.getTxId()).isNull();
        assertThat(result.getPixCopyPaste()).isNotBlank();
    }

    @Test
    void preview_nonPositiveAmount_throwsBusinessRule() {
        PixPreviewDTO dto =
                new PixPreviewDTO("11999998888", "Barber", new BigDecimal("0.00"), null);

        assertThrows(BusinessRuleException.class, () -> pixService.preview(dto));
    }
}
