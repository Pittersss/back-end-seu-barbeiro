package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.appointment.AppointmentPostDTO;
import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.AppointmentMapper;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.AppointmentRepository;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.ClientBlockRepository;
import com.two_m.yourbarber.repository.ClientRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;
    private final ServiceRepository serviceRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final ClientBlockRepository clientBlockRepository;

    @Override
    public AppointmentResponseDTO createAppointment(AppointmentPostDTO dto, Long clientId) {
        Client client = findClient(clientId);
        Barber barber = findBarber(dto.getBarberId());
        com.two_m.yourbarber.model.Service service = findService(dto.getServiceId());

        if (!barber.isAvailable()) {
            throw new BusinessRuleException("Barber is not currently available");
        }
        if (!service.isAvailable()) {
            throw new BusinessRuleException("Service is not currently available");
        }
        if (barber.getBarberShop() == null
                || service.getBarberShop() == null
                || !barber.getBarberShop().getId().equals(service.getBarberShop().getId())) {
            throw new BusinessRuleException("Barber does not offer this service");
        }
        if (service.getBarber() != null && !service.getBarber().getId().equals(barber.getId())) {
            throw new BusinessRuleException("Barber does not offer this service");
        }
        if (clientBlockRepository.existsByBarberIdAndClientId(barber.getId(), client.getId())) {
            throw new ForbiddenOperationException(
                    "Você não pode agendar com este barbeiro no momento.");
        }
        LocalDateTime start = dto.getScheduledAt();
        LocalDateTime end = start.plusMinutes(durationOf(service));
        if (isOutsideWorkingHours(barber, start, end)) {
            throw new BusinessRuleException("Fora do horário de atendimento do barbeiro.");
        }
        if (overlapsBreak(barber, start, end)) {
            throw new BusinessRuleException("O barbeiro está em intervalo nesse horário.");
        }
        if (overlapsTimeBlock(barber.getId(), start, end)) {
            throw new BusinessRuleException("O barbeiro bloqueou esse horário.");
        }
        if (hasSchedulingConflict(barber.getId(), dto.getScheduledAt(), service)) {
            throw new BusinessRuleException("Barber already has an appointment at that time");
        }

        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(dto.getScheduledAt())
                        .paymentMethod(dto.getPaymentMethod())
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();

        return AppointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForUser(Long userId, UserRole role) {
        List<Appointment> appointments =
                switch (role) {
                    case CLIENT -> appointmentRepository.findByClientId(userId);
                    case BARBER -> appointmentRepository.findByBarberId(userId);
                    case ADMIN -> appointmentRepository.findAll();
                };
        return appointments.stream().map(AppointmentMapper::toDto).toList();
    }

    @Override
    public AppointmentResponseDTO updateStatus(
            Long appointmentId, AppointmentStatus status, Long requesterId) {
        Appointment appointment = findAppointment(appointmentId);
        if (!appointment.getBarber().getId().equals(requesterId)) {
            throw new ForbiddenOperationException(
                    "Only the assigned barber can update this appointment's status");
        }
        appointment.setStatus(status);
        return AppointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Override
    public void cancelAppointment(Long appointmentId, Long requesterId) {
        Appointment appointment = findAppointment(appointmentId);
        boolean isParticipant =
                appointment.getClient().getId().equals(requesterId)
                        || appointment.getBarber().getId().equals(requesterId);
        if (!isParticipant) {
            throw new ForbiddenOperationException("You are not part of this appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Este agendamento não pode mais ser cancelado.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    private int durationOf(com.two_m.yourbarber.model.Service service) {
        return service.getDurationMinutes() != null && service.getDurationMinutes() > 0
                ? service.getDurationMinutes()
                : 30;
    }

    private boolean isOutsideWorkingHours(Barber barber, LocalDateTime start, LocalDateTime end) {
        LocalDateTime dayStart = start.toLocalDate().atTime(barber.getWorkStartHour(), 0);
        LocalDateTime dayEnd =
                barber.getWorkEndHour() >= 24
                        ? start.toLocalDate().plusDays(1).atStartOfDay()
                        : start.toLocalDate().atTime(barber.getWorkEndHour(), 0);
        return start.isBefore(dayStart) || end.isAfter(dayEnd);
    }

    private boolean overlapsBreak(Barber barber, LocalDateTime start, LocalDateTime end) {
        if (barber.getBreakStartHour() == null || barber.getBreakEndHour() == null) {
            return false;
        }
        LocalDateTime breakStart = start.toLocalDate().atTime(barber.getBreakStartHour(), 0);
        LocalDateTime breakEnd = start.toLocalDate().atTime(barber.getBreakEndHour(), 0);
        return start.isBefore(breakEnd) && breakStart.isBefore(end);
    }

    private boolean overlapsTimeBlock(Long barberId, LocalDateTime start, LocalDateTime end) {
        return timeBlockRepository.findByBarberId(barberId).stream()
                .anyMatch(b -> start.isBefore(b.getEndsAt()) && b.getStartsAt().isBefore(end));
    }

    private boolean hasSchedulingConflict(
            Long barberId,
            LocalDateTime newStart,
            com.two_m.yourbarber.model.Service newService) {
        LocalDateTime newEnd = newStart.plusMinutes(newService.getDurationMinutes());

        return appointmentRepository.findByBarberId(barberId).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .anyMatch(
                        a -> {
                            LocalDateTime existingStart = a.getScheduledAt();
                            LocalDateTime existingEnd =
                                    existingStart.plusMinutes(
                                            a.getService().getDurationMinutes());
                            return existingStart.isBefore(newEnd)
                                    && newStart.isBefore(existingEnd);
                        });
    }

    private Client findClient(Long id) {
        return clientRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private Barber findBarber(Long id) {
        return barberRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found: " + id));
    }

    private com.two_m.yourbarber.model.Service findService(Long id) {
        return serviceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Appointment not found: " + id));
    }
}
