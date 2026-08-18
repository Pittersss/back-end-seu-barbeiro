package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.appointment.AppointmentPostDTO;
import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.model.enums.UserRole;
import java.util.List;

public interface AppointmentService {

    AppointmentResponseDTO createAppointment(AppointmentPostDTO dto, Long clientId);

    List<AppointmentResponseDTO> getAppointmentsForUser(Long userId, UserRole role);

    AppointmentResponseDTO updateStatus(
            Long appointmentId, AppointmentStatus status, Long requesterId);

    void cancelAppointment(Long appointmentId, Long requesterId);
}
