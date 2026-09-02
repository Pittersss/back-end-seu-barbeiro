package com.two_m.yourbarber.service.pix;

import com.two_m.yourbarber.dto.pix.PixPreviewDTO;
import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.model.enums.PaymentMethod;
import com.two_m.yourbarber.repository.AppointmentRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PixServiceImpl implements PixService {

    /**
     * The domain model has no dedicated "city" field for a barbershop (only a free-text
     * address), so the mandatory EMV merchant-city field falls back to this constant. Wallets do
     * not validate this value against the payer's location, so it does not affect payment.
     */
    private static final String DEFAULT_MERCHANT_CITY = "BRASIL";

    private static final String TXID_PREFIX = "APT";

    private final AppointmentRepository appointmentRepository;

    @Override
    public PixQrCodeResponseDTO generateQrCode(Long appointmentId, Long requesterId) {
        Appointment appointment = findAppointment(appointmentId);

        boolean isParticipant =
                appointment.getClient().getId().equals(requesterId)
                        || appointment.getBarber().getId().equals(requesterId);
        if (!isParticipant) {
            throw new ForbiddenOperationException("You are not part of this appointment");
        }
        if (appointment.getPaymentMethod() != PaymentMethod.PIX) {
            throw new BusinessRuleException("This appointment is not set up for Pix payment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Cannot generate a payment code for a finished appointment");
        }

        Barber barber = appointment.getBarber();
        if (barber.getPixKey() == null || barber.getPixKey().isBlank()) {
            throw new BusinessRuleException("Barber has not configured a Pix key");
        }

        BigDecimal amount = appointment.getService().getPrice();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Service has no price configured");
        }

        String merchantName = barber.getName();
        String txId = TXID_PREFIX + appointment.getId();

        String pixCopyPaste =
                PixBrCodeGenerator.generate(
                        barber.getPixKey(), merchantName, DEFAULT_MERCHANT_CITY, amount, txId);
        String qrCodeBase64 = PixQrCodeImageGenerator.toPngBase64(pixCopyPaste);

        return PixQrCodeResponseDTO.builder()
                .appointmentId(appointment.getId())
                .pixKey(barber.getPixKey())
                .amount(amount)
                .merchantName(merchantName)
                .merchantCity(DEFAULT_MERCHANT_CITY)
                .txId(txId)
                .pixCopyPaste(pixCopyPaste)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    @Override
    public PixQrCodeResponseDTO preview(PixPreviewDTO dto) {
        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero");
        }

        String txId = dto.getTxId() == null || dto.getTxId().isBlank() ? null : dto.getTxId();

        String pixCopyPaste =
                PixBrCodeGenerator.generate(
                        dto.getPixKey(),
                        dto.getMerchantName(),
                        DEFAULT_MERCHANT_CITY,
                        amount,
                        txId);
        String qrCodeBase64 = PixQrCodeImageGenerator.toPngBase64(pixCopyPaste);

        return PixQrCodeResponseDTO.builder()
                .pixKey(dto.getPixKey().trim())
                .amount(amount)
                .merchantName(dto.getMerchantName().trim())
                .merchantCity(DEFAULT_MERCHANT_CITY)
                .txId(txId)
                .pixCopyPaste(pixCopyPaste)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Appointment not found: " + id));
    }
}
