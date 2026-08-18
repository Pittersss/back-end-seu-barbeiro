package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.model.Appointment;

public final class AppointmentMapper {

    private AppointmentMapper() {}

    public static AppointmentResponseDTO toDto(Appointment appointment) {
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .scheduledAt(appointment.getScheduledAt())
                .status(appointment.getStatus())
                .paymentMethod(appointment.getPaymentMethod())
                .clientId(appointment.getClient().getId())
                .clientName(appointment.getClient().getName())
                .barberId(appointment.getBarber().getId())
                .barberName(appointment.getBarber().getName())
                .serviceId(appointment.getService().getId())
                .serviceName(appointment.getService().getName())
                .build();
    }
}
