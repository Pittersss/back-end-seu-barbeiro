package com.two_m.yourbarber.service;

import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.TimeBlock;
import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.repository.AppointmentRepository;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final int SLOT_STEP_MINUTES = 30;
    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final int MAX_RANGE_DAYS = 31;

    private final BarberRepository barberRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final TimeBlockRepository timeBlockRepository;

    @Override
    public Map<LocalDate, List<LocalDateTime>> openSlots(
            Long barberId, Long serviceId, LocalDate from, LocalDate to) {

        Barber barber =
                barberRepository
                        .findById(barberId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Barber not found: " + barberId));
        com.two_m.yourbarber.model.Service service =
                serviceRepository
                        .findById(serviceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Service not found: " + serviceId));

        int duration =
                service.getDurationMinutes() != null && service.getDurationMinutes() > 0
                        ? service.getDurationMinutes()
                        : DEFAULT_DURATION_MINUTES;

        LocalDate today = LocalDate.now();
        LocalDate start = from.isBefore(today) ? today : from;
        LocalDate end = to.isBefore(start) ? start : to;
        if (end.isAfter(start.plusDays(MAX_RANGE_DAYS))) {
            end = start.plusDays(MAX_RANGE_DAYS);
        }

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<Appointment> appointments =
                appointmentRepository
                        .findByBarberIdAndScheduledAtBetween(barberId, rangeStart, rangeEnd)
                        .stream()
                        .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                        .toList();
        List<TimeBlock> blocks =
                timeBlockRepository.findByBarberId(barberId).stream()
                        .filter(
                                b ->
                                        b.getEndsAt().isAfter(rangeStart)
                                                && b.getStartsAt().isBefore(rangeEnd))
                        .toList();

        Map<LocalDate, List<LocalDateTime>> result = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            List<LocalDateTime> slots = slotsForDay(day, barber, duration, now, appointments, blocks);
            if (!slots.isEmpty()) {
                result.put(day, slots);
            }
        }
        return result;
    }

    private List<LocalDateTime> slotsForDay(
            LocalDate day,
            Barber barber,
            int duration,
            LocalDateTime now,
            List<Appointment> appointments,
            List<TimeBlock> blocks) {

        LocalDateTime workStart = day.atTime(Math.min(barber.getWorkStartHour(), 23), 0);
        LocalDateTime workEnd =
                barber.getWorkEndHour() >= 24
                        ? day.plusDays(1).atStartOfDay()
                        : day.atTime(barber.getWorkEndHour(), 0);

        LocalDateTime breakStart = null;
        LocalDateTime breakEnd = null;
        if (barber.getBreakStartHour() != null && barber.getBreakEndHour() != null) {
            breakStart = day.atTime(barber.getBreakStartHour(), 0);
            breakEnd = day.atTime(barber.getBreakEndHour(), 0);
        }

        List<LocalDateTime> slots = new ArrayList<>();
        for (LocalDateTime cursor = workStart;
                !cursor.plusMinutes(duration).isAfter(workEnd);
                cursor = cursor.plusMinutes(SLOT_STEP_MINUTES)) {

            final LocalDateTime slot = cursor;
            final LocalDateTime slotEnd = slot.plusMinutes(duration);
            if (slot.isBefore(now)) {
                continue;
            }
            if (breakStart != null && overlaps(slot, slotEnd, breakStart, breakEnd)) {
                continue;
            }
            boolean blocked =
                    blocks.stream()
                            .anyMatch(b -> overlaps(slot, slotEnd, b.getStartsAt(), b.getEndsAt()));
            if (blocked) {
                continue;
            }
            boolean taken =
                    appointments.stream()
                            .anyMatch(
                                    a ->
                                            overlaps(
                                                    slot,
                                                    slotEnd,
                                                    a.getScheduledAt(),
                                                    a.getScheduledAt()
                                                            .plusMinutes(apptDuration(a))));
            if (taken) {
                continue;
            }
            slots.add(slot);
        }
        return slots;
    }

    private static int apptDuration(Appointment appointment) {
        Integer minutes = appointment.getService().getDurationMinutes();
        return minutes != null && minutes > 0 ? minutes : DEFAULT_DURATION_MINUTES;
    }

    private static boolean overlaps(
            LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
