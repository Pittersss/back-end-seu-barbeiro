package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.appointment.AppointmentPostDTO;
import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.enums.AppointmentStatus;
import com.two_m.yourbarber.model.enums.PaymentMethod;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.AppointmentRepository;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.ClientBlockRepository;
import com.two_m.yourbarber.repository.ClientRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private BarberRepository barberRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private TimeBlockRepository timeBlockRepository;
    @Mock private ClientBlockRepository clientBlockRepository;

    @InjectMocks private AppointmentServiceImpl appointmentService;

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

    private Barber barber(long id, BarberShop shop, boolean available) {
        Barber barber =
                Barber.builder()
                        .name("Barber")
                        .email("barber@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .available(available)
                        .workStartHour(0)
                        .workEndHour(24)
                        .barberShop(shop)
                        .build();
        barber.setId(id);
        return barber;
    }

    private com.two_m.yourbarber.model.Service offering(
            long id, BarberShop shop, boolean available) {
        com.two_m.yourbarber.model.Service service =
                com.two_m.yourbarber.model.Service.builder()
                        .name("Haircut")
                        .durationMinutes(30)
                        .price(BigDecimal.valueOf(50))
                        .available(available)
                        .barberShop(shop)
                        .build();
        service.setId(id);
        return service;
    }

    @Test
    void createAppointment_valid_savesAppointment() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        shop.setId(9L);
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(barber));
        when(serviceRepository.findById(3L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findByBarberId(2L)).thenReturn(List.of());
        when(timeBlockRepository.findByBarberId(2L)).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentPostDTO dto =
                new AppointmentPostDTO(
                        2L,
                        3L,
                        LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
                        PaymentMethod.PIX);
        AppointmentResponseDTO result = appointmentService.createAppointment(dto, 1L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(result.getBarberId()).isEqualTo(2L);
    }

    @Test
    void createAppointment_barberUnavailable_throws() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        shop.setId(9L);
        Client client = client(1L);
        Barber barber = barber(2L, shop, false);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(barber));
        when(serviceRepository.findById(3L)).thenReturn(Optional.of(service));

        AppointmentPostDTO dto =
                new AppointmentPostDTO(
                        2L, 3L, LocalDateTime.now().plusDays(1), PaymentMethod.PIX);

        assertThrows(
                BusinessRuleException.class,
                () -> appointmentService.createAppointment(dto, 1L));
    }

    @Test
    void createAppointment_serviceAssignedToOtherBarber_throws() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        shop.setId(9L);
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        Barber otherBarber = barber(4L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);
        service.setBarber(otherBarber);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(barber));
        when(serviceRepository.findById(3L)).thenReturn(Optional.of(service));

        AppointmentPostDTO dto =
                new AppointmentPostDTO(
                        2L, 3L, LocalDateTime.now().plusDays(1), PaymentMethod.PIX);

        assertThrows(
                BusinessRuleException.class,
                () -> appointmentService.createAppointment(dto, 1L));
    }

    @Test
    void createAppointment_conflictingTime_throws() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        shop.setId(9L);
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);

        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1);
        Appointment existing =
                Appointment.builder()
                        .scheduledAt(scheduledAt)
                        .status(AppointmentStatus.CONFIRMED)
                        .barber(barber)
                        .service(service)
                        .client(client)
                        .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(barber));
        when(serviceRepository.findById(3L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findByBarberId(2L)).thenReturn(List.of(existing));
        when(timeBlockRepository.findByBarberId(2L)).thenReturn(List.of());

        AppointmentPostDTO dto =
                new AppointmentPostDTO(2L, 3L, scheduledAt.plusMinutes(10), PaymentMethod.PIX);

        assertThrows(
                BusinessRuleException.class,
                () -> appointmentService.createAppointment(dto, 1L));
    }

    @Test
    void createAppointment_clientBlockedByBarber_throwsForbidden() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        shop.setId(9L);
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(barber));
        when(serviceRepository.findById(3L)).thenReturn(Optional.of(service));
        when(clientBlockRepository.existsByBarberIdAndClientId(2L, 1L)).thenReturn(true);

        AppointmentPostDTO dto =
                new AppointmentPostDTO(
                        2L, 3L, LocalDateTime.now().plusDays(1), PaymentMethod.PIX);

        assertThrows(
                ForbiddenOperationException.class,
                () -> appointmentService.createAppointment(dto, 1L));
    }

    @Test
    void createAppointment_insideTimeBlock_throws() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        shop.setId(9L);
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        com.two_m.yourbarber.model.TimeBlock block =
                com.two_m.yourbarber.model.TimeBlock.builder()
                        .barber(barber)
                        .startsAt(start.minusHours(1))
                        .endsAt(start.plusHours(1))
                        .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(barber));
        when(serviceRepository.findById(3L)).thenReturn(Optional.of(service));
        when(timeBlockRepository.findByBarberId(2L)).thenReturn(List.of(block));

        AppointmentPostDTO dto = new AppointmentPostDTO(2L, 3L, start, PaymentMethod.PIX);

        assertThrows(
                BusinessRuleException.class,
                () -> appointmentService.createAppointment(dto, 1L));
    }

    @Test
    void getAppointmentsForUser_client_usesClientLookup() {
        when(appointmentRepository.findByClientId(1L)).thenReturn(List.of());

        List<AppointmentResponseDTO> result =
                appointmentService.getAppointmentsForUser(1L, UserRole.CLIENT);

        assertThat(result).isEmpty();
    }

    @Test
    void updateStatus_assignedBarber_updatesStatus() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);
        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(LocalDateTime.now().minusHours(1))
                        .status(AppointmentStatus.CONFIRMED)
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();
        appointment.setId(7L);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponseDTO result =
                appointmentService.updateStatus(7L, AppointmentStatus.COMPLETED, 2L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void updateStatus_completeBeforeScheduledTime_throwsBusinessRule() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);
        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(LocalDateTime.now().plusDays(1))
                        .status(AppointmentStatus.CONFIRMED)
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();
        appointment.setId(7L);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(
                BusinessRuleException.class,
                () -> appointmentService.updateStatus(7L, AppointmentStatus.COMPLETED, 2L));
    }

    @Test
    void updateStatus_notAssignedBarber_throwsForbidden() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);
        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(LocalDateTime.now().plusDays(1))
                        .status(AppointmentStatus.PENDING)
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();
        appointment.setId(7L);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(
                ForbiddenOperationException.class,
                () -> appointmentService.updateStatus(7L, AppointmentStatus.COMPLETED, 99L));
    }

    @Test
    void cancelAppointment_client_setsCancelled() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);
        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(LocalDateTime.now().plusDays(1))
                        .status(AppointmentStatus.PENDING)
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();
        appointment.setId(7L);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.cancelAppointment(7L, 1L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancelAppointment_nonParticipant_throwsForbidden() {
        BarberShop shop = BarberShop.builder().name("Shop").build();
        Client client = client(1L);
        Barber barber = barber(2L, shop, true);
        com.two_m.yourbarber.model.Service service = offering(3L, shop, true);
        Appointment appointment =
                Appointment.builder()
                        .scheduledAt(LocalDateTime.now().plusDays(1))
                        .status(AppointmentStatus.PENDING)
                        .client(client)
                        .barber(barber)
                        .service(service)
                        .build();
        appointment.setId(7L);

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThrows(
                ForbiddenOperationException.class,
                () -> appointmentService.cancelAppointment(7L, 99L));
    }
}
