package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.AppointmentMapper;
import com.two_m.yourbarber.mapper.BarberShopMapper;
import com.two_m.yourbarber.mapper.UserMapper;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.BarberShopRequest;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.enums.NotificationType;
import com.two_m.yourbarber.model.enums.RequestStatus;
import com.two_m.yourbarber.repository.AppointmentRepository;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.BarberShopRequestRepository;
import com.two_m.yourbarber.repository.ClientBlockRepository;
import com.two_m.yourbarber.repository.ClientRepository;
import com.two_m.yourbarber.repository.JoinRequestRepository;
import com.two_m.yourbarber.repository.NotificationRepository;
import com.two_m.yourbarber.repository.PushSubscriptionRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import com.two_m.yourbarber.repository.SubscriptionPaymentRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final BarberShopRequestRepository barberShopRequestRepository;
    private final BarberShopRepository barberShopRepository;
    private final BarberRepository barberRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ClientBlockRepository clientBlockRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ServiceRepository serviceRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final NotificationService notificationService;

    @Override
    public List<BarberShopRequestResponseDTO> listPendingRequests() {
        return barberShopRequestRepository.findByStatus(RequestStatus.PENDING).stream()
                .map(BarberShopMapper::toRequestDto)
                .toList();
    }

    @Override
    public BarberShopRequestResponseDTO decideRequest(Long requestId, boolean approved) {
        BarberShopRequest request =
                barberShopRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Barbershop request not found: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("This request has already been reviewed");
        }

        request.setStatus(approved ? RequestStatus.APPROVED : RequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());

        if (approved) {
            Barber owner = request.getRequester();
            BarberShop shop =
                    barberShopRepository.save(
                            BarberShop.builder()
                                    .name(request.getShopName())
                                    .address(request.getShopAddress())
                                    .phone(request.getShopPhone())
                                    .owner(owner)
                                    .build());
            owner.setBarberShop(shop);
            barberRepository.save(owner);
        }

        BarberShopRequestResponseDTO responseDto =
                BarberShopMapper.toRequestDto(barberShopRequestRepository.save(request));

        String message =
                approved
                        ? "Sua barbearia \"" + request.getShopName() + "\" foi aprovada."
                        : "Sua solicitação de criação da barbearia \""
                                + request.getShopName()
                                + "\" foi rejeitada.";
        notificationService.notify(
                request.getRequester().getId(),
                NotificationType.BARBERSHOP_REQUEST_DECIDED,
                message,
                null);

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDTO> listClients() {
        return clientRepository.findAll().stream().map(UserMapper::toProfileDto).toList();
    }

    @Override
    public void deleteClient(Long clientId) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Client not found: " + clientId));

        deleteAppointments(appointmentRepository.findByClientId(clientId));
        notificationRepository.deleteByRecipientId(clientId);
        pushSubscriptionRepository.deleteByUserId(clientId);
        clientBlockRepository.deleteByClientId(clientId);

        clientRepository.delete(client);
    }

    @Override
    public void deleteBarberShop(Long shopId) {
        BarberShop shop =
                barberShopRepository
                        .findById(shopId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Barbershop not found: " + shopId));

        Barber owner = shop.getOwner();

        // Appointments point at the shop's services (NO ACTION FK), so they must go
        // before the cascade removes the services.
        deleteAppointments(appointmentRepository.findByServiceBarberShopId(shopId));

        for (Barber member : List.copyOf(shop.getBarbers())) {
            member.setBarberShop(null);
            if (owner != null && member.getId().equals(owner.getId())) {
                member.setBlockedFromOwning(true);
            }
            barberRepository.save(member);
        }
        if (owner != null && owner.getBarberShop() != null) {
            // Owner wasn't in the barbers collection for some reason — handle separately
            // so they're still blocked from creating another shop.
            owner.setBarberShop(null);
            owner.setBlockedFromOwning(true);
            barberRepository.save(owner);
        }

        joinRequestRepository.deleteByBarberShopId(shopId);
        barberShopRepository.delete(shop);
        barberShopRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDTO> listBarbers() {
        return barberRepository.findAll().stream().map(UserMapper::toProfileDto).toList();
    }

    @Override
    public void deleteBarber(Long barberId) {
        Barber barber =
                barberRepository
                        .findById(barberId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Barber not found: " + barberId));

        BarberShop shop = barber.getBarberShop();
        if (shop != null && shop.getOwner() != null && shop.getOwner().getId().equals(barberId)) {
            deleteBarberShop(shop.getId());
        } else if (shop != null) {
            barber.setBarberShop(null);
            barberRepository.save(barber);
        }

        deleteAppointments(appointmentRepository.findByBarberId(barberId));
        for (com.two_m.yourbarber.model.Service service : serviceRepository.findByBarberId(barberId)) {
            deleteAppointments(appointmentRepository.findByServiceId(service.getId()));
            serviceRepository.delete(service);
        }
        serviceRepository.flush();

        timeBlockRepository.deleteByBarberId(barberId);
        clientBlockRepository.deleteByBarberId(barberId);
        subscriptionPaymentRepository.deleteByBarberId(barberId);
        joinRequestRepository.deleteByBarberId(barberId);
        barberShopRequestRepository.deleteByRequesterId(barberId);
        notificationRepository.deleteByRecipientId(barberId);
        pushSubscriptionRepository.deleteByUserId(barberId);

        barberRepository.delete(barber);
    }

    private void deleteAppointments(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return;
        }
        notificationRepository.deleteByAppointmentIdIn(
                appointments.stream().map(Appointment::getId).toList());
        appointmentRepository.deleteAll(appointments);
        appointmentRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> listAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(AppointmentMapper::toDto);
    }
}
