package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.TimeBlock;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.AppointmentRepository;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    @Mock private BarberRepository barberRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TimeBlockRepository timeBlockRepository;

    @InjectMocks private AvailabilityServiceImpl service;

    private Barber barber(int start, int end, Integer breakStart, Integer breakEnd) {
        Barber barber =
                Barber.builder()
                        .name("B")
                        .email("b@x.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .workStartHour(start)
                        .workEndHour(end)
                        .breakStartHour(breakStart)
                        .breakEndHour(breakEnd)
                        .build();
        barber.setId(1L);
        return barber;
    }

    private com.two_m.yourbarber.model.Service service(int durationMinutes) {
        return com.two_m.yourbarber.model.Service.builder()
                .name("Cut")
                .durationMinutes(durationMinutes)
                .build();
    }

    @Test
    void openSlots_serviceAssignedToOtherBarber_returnsEmpty() {
        Barber otherBarber = barber(9, 12, null, null);
        otherBarber.setId(2L);
        com.two_m.yourbarber.model.Service assignedService = service(30);
        assignedService.setBarber(otherBarber);

        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber(9, 12, null, null)));
        when(serviceRepository.findById(7L)).thenReturn(Optional.of(assignedService));

        LocalDate day = LocalDate.now().plusDays(3);
        assertThat(service.openSlots(1L, 7L, day, day)).isEmpty();
    }

    @Test
    void openSlots_plainWindow_returnsEverySlotThatFits() {
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber(9, 12, null, null)));
        when(serviceRepository.findById(7L)).thenReturn(Optional.of(service(30)));
        when(appointmentRepository.findByBarberIdAndScheduledAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(timeBlockRepository.findByBarberId(1L)).thenReturn(List.of());

        LocalDate day = LocalDate.now().plusDays(3);
        List<LocalDateTime> slots = service.openSlots(1L, 7L, day, day).get(day);

        assertThat(slots).containsExactly(
                day.atTime(9, 0),
                day.atTime(9, 30),
                day.atTime(10, 0),
                day.atTime(10, 30),
                day.atTime(11, 0),
                day.atTime(11, 30));
    }

    @Test
    void openSlots_withBreakAndBlock_excludesThem() {
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber(9, 13, 12, 13)));
        when(serviceRepository.findById(7L)).thenReturn(Optional.of(service(30)));
        when(appointmentRepository.findByBarberIdAndScheduledAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        LocalDate day = LocalDate.now().plusDays(3);
        TimeBlock block =
                TimeBlock.builder()
                        .barber(barber(9, 13, 12, 13))
                        .startsAt(day.atTime(10, 0))
                        .endsAt(day.atTime(11, 0))
                        .build();
        when(timeBlockRepository.findByBarberId(1L)).thenReturn(List.of(block));

        List<LocalDateTime> slots = service.openSlots(1L, 7L, day, day).get(day);

        // 09:00, 09:30, 11:00, 11:30 — 10:00/10:30 blocked, 12:00+ is the lunch break
        assertThat(slots).containsExactly(
                day.atTime(9, 0), day.atTime(9, 30), day.atTime(11, 0), day.atTime(11, 30));
    }
}
