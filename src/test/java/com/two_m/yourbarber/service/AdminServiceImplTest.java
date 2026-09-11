package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.BarberShopRequest;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.enums.RequestStatus;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.AppointmentRepository;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.BarberShopRequestRepository;
import com.two_m.yourbarber.repository.ClientBlockRepository;
import com.two_m.yourbarber.repository.ClientRepository;
import com.two_m.yourbarber.repository.JoinRequestRepository;
import com.two_m.yourbarber.repository.NotificationRepository;
import com.two_m.yourbarber.repository.PushSubscriptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private BarberShopRequestRepository barberShopRequestRepository;
    @Mock private BarberShopRepository barberShopRepository;
    @Mock private BarberRepository barberRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock private ClientBlockRepository clientBlockRepository;
    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private AdminServiceImpl adminService;

    private Barber requester(long id) {
        Barber barber =
                Barber.builder()
                        .name("Requester")
                        .email("requester@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .build();
        barber.setId(id);
        return barber;
    }

    @Test
    void decideRequest_approved_createsShopAndAssignsOwner() {
        Barber requester = requester(1L);
        BarberShopRequest request =
                BarberShopRequest.builder()
                        .status(RequestStatus.PENDING)
                        .shopName("New Shop")
                        .shopAddress("Address")
                        .requester(requester)
                        .build();
        request.setId(20L);

        when(barberShopRequestRepository.findById(20L)).thenReturn(Optional.of(request));
        when(barberShopRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            BarberShop shop = inv.getArgument(0);
                            shop.setId(100L);
                            return shop;
                        });
        when(barberShopRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BarberShopRequestResponseDTO result = adminService.decideRequest(20L, true);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(requester.getBarberShop()).isNotNull();
        assertThat(requester.getBarberShop().getId()).isEqualTo(100L);
        verify(barberRepository).save(requester);
    }

    @Test
    void decideRequest_rejected_doesNotCreateShop() {
        Barber requester = requester(1L);
        BarberShopRequest request =
                BarberShopRequest.builder()
                        .status(RequestStatus.PENDING)
                        .shopName("New Shop")
                        .requester(requester)
                        .build();
        request.setId(20L);

        when(barberShopRequestRepository.findById(20L)).thenReturn(Optional.of(request));
        when(barberShopRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BarberShopRequestResponseDTO result = adminService.decideRequest(20L, false);

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        verify(barberShopRepository, never()).save(any());
        assertThat(requester.getBarberShop()).isNull();
    }

    @Test
    void decideRequest_alreadyReviewed_throws() {
        Barber requester = requester(1L);
        BarberShopRequest request =
                BarberShopRequest.builder()
                        .status(RequestStatus.APPROVED)
                        .shopName("New Shop")
                        .requester(requester)
                        .build();
        request.setId(20L);

        when(barberShopRequestRepository.findById(20L)).thenReturn(Optional.of(request));

        assertThrows(
                BusinessRuleException.class, () -> adminService.decideRequest(20L, true));
    }

    @Test
    void decideRequest_notFound_throws() {
        when(barberShopRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.decideRequest(99L, true));
    }

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

    @Test
    void listClients_mapsAllClients() {
        when(clientRepository.findAll()).thenReturn(List.of(client(1L), client(2L)));

        List<UserProfileDTO> result = adminService.listClients();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void deleteClient_removesAppointmentsNotificationsAndClient() {
        Client client = client(1L);
        Appointment appointment = Appointment.builder().client(client).build();
        appointment.setId(5L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(appointmentRepository.findByClientId(1L)).thenReturn(List.of(appointment));

        adminService.deleteClient(1L);

        verify(notificationRepository).deleteByAppointmentIdIn(List.of(5L));
        verify(notificationRepository).deleteByRecipientId(1L);
        verify(appointmentRepository).deleteAll(List.of(appointment));
        verify(pushSubscriptionRepository).deleteByUserId(1L);
        verify(clientBlockRepository).deleteByClientId(1L);
        verify(clientRepository).delete(client);
    }

    @Test
    void deleteClient_noAppointments_skipsAppointmentNotificationCleanup() {
        Client client = client(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(appointmentRepository.findByClientId(1L)).thenReturn(List.of());

        adminService.deleteClient(1L);

        verify(notificationRepository, never()).deleteByAppointmentIdIn(anyList());
        verify(notificationRepository).deleteByRecipientId(1L);
        verify(clientRepository).delete(client);
    }

    @Test
    void deleteClient_notFound_throws() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.deleteClient(99L));
    }

    @Test
    void deleteBarberShop_detachesBarbersAndBlocksOwner() {
        Barber owner = requester(1L);
        BarberShop shop = BarberShop.builder().name("Shop").owner(owner).build();
        shop.setId(5L);
        Barber member = requester(2L);
        member.setBarberShop(shop);
        owner.setBarberShop(shop);
        shop.setBarbers(List.of(owner, member));

        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        adminService.deleteBarberShop(5L);

        assertThat(owner.getBarberShop()).isNull();
        assertThat(member.getBarberShop()).isNull();
        assertThat(owner.isBlockedFromOwning()).isTrue();
        verify(barberRepository, times(2)).save(any());
        verify(joinRequestRepository).deleteByBarberShopId(5L);
        verify(barberShopRepository).delete(shop);
    }

    @Test
    void deleteBarberShop_notFound_throws() {
        when(barberShopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> adminService.deleteBarberShop(99L));
    }

    @Test
    void listAppointments_delegatesToRepositoryAndMaps() {
        Barber barber = requester(1L);
        Client client = client(2L);
        com.two_m.yourbarber.model.Service service =
                com.two_m.yourbarber.model.Service.builder().name("Corte").price(new java.math.BigDecimal("10")).build();
        service.setId(3L);
        Appointment appointment =
                Appointment.builder().client(client).barber(barber).service(service).build();
        appointment.setId(9L);
        Pageable pageable = PageRequest.of(0, 20);
        when(appointmentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(appointment)));

        var result = adminService.listAppointments(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(9L);
    }
}
