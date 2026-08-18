package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.appointment.AppointmentPostDTO;
import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.dto.appointment.AppointmentStatusDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(
            @Valid @RequestBody AppointmentPostDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(dto, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointments(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                appointmentService.getAppointmentsForUser(
                        currentUser.getId(), currentUser.getRole()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                appointmentService.updateStatus(id, dto.getStatus(), currentUser.getId()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        appointmentService.cancelAppointment(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
