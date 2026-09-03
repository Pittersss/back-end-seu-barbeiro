package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.service.ServicePostPutDTO;
import com.two_m.yourbarber.dto.service.ServiceResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private BarberShopRepository barberShopRepository;
    @Mock private BarberRepository barberRepository;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private ServiceServiceImpl serviceService;

    private Barber owner(long id) {
        Barber barber =
                Barber.builder()
                        .name("Owner")
                        .email("owner@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .build();
        barber.setId(id);
        return barber;
    }

    private Barber member(long id, BarberShop shop) {
        Barber barber =
                Barber.builder()
                        .name("Member")
                        .email("member@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .barberShop(shop)
                        .build();
        barber.setId(id);
        return barber;
    }

    private BarberShop shop(long id, Barber owner) {
        BarberShop shop = BarberShop.builder().name("Shop").owner(owner).build();
        shop.setId(id);
        owner.setBarberShop(shop);
        return shop;
    }

    private com.two_m.yourbarber.model.Service offering(long id, BarberShop shop) {
        return offering(id, shop, null);
    }

    private com.two_m.yourbarber.model.Service offering(long id, BarberShop shop, Barber barber) {
        com.two_m.yourbarber.model.Service service =
                com.two_m.yourbarber.model.Service.builder()
                        .name("Haircut")
                        .durationMinutes(30)
                        .price(BigDecimal.valueOf(50))
                        .available(true)
                        .barberShop(shop)
                        .barber(barber)
                        .build();
        service.setId(id);
        return service;
    }

    @Test
    void createService_subscriptionInactive_throws() {
        Barber owner = owner(1L);
        shop(5L, owner);
        org.mockito.Mockito.doThrow(
                        new com.two_m.yourbarber.exception.SubscriptionRequiredException("inactive"))
                .when(subscriptionService)
                .assertActive(1L);

        ServicePostPutDTO dto =
                new ServicePostPutDTO("Haircut", "desc", 30, BigDecimal.valueOf(50), null);

        assertThrows(
                com.two_m.yourbarber.exception.SubscriptionRequiredException.class,
                () -> serviceService.createService(5L, dto, 1L));
    }

    @Test
    void createService_owner_savesService() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServicePostPutDTO dto =
                new ServicePostPutDTO("Haircut", "desc", 30, BigDecimal.valueOf(50), null);
        ServiceResponseDTO result = serviceService.createService(5L, dto, 1L);

        assertThat(result.getName()).isEqualTo("Haircut");
        assertThat(result.getBarberShopId()).isEqualTo(5L);
        assertThat(result.getBarberId()).isEqualTo(1L);
    }

    @Test
    void createService_memberBarber_savesService() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        Barber member = member(2L, shop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServicePostPutDTO dto =
                new ServicePostPutDTO("Beard trim", "desc", 20, BigDecimal.valueOf(30), null);
        ServiceResponseDTO result = serviceService.createService(5L, dto, 2L);

        assertThat(result.getBarberId()).isEqualTo(2L);
    }

    @Test
    void createService_notMember_throwsForbidden() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        ServicePostPutDTO dto =
                new ServicePostPutDTO("Haircut", "desc", 30, BigDecimal.valueOf(50), null);

        assertThrows(
                ForbiddenOperationException.class,
                () -> serviceService.createService(5L, dto, 2L));
    }

    @Test
    void listServices_returnsShopServices() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.findByBarberShopId(5L))
                .thenReturn(List.of(offering(20L, shop)));

        List<ServiceResponseDTO> result = serviceService.listServices(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(20L);
    }

    @Test
    void toggleAvailability_ownerAndServiceInShop_flipsFlag() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        com.two_m.yourbarber.model.Service service = offering(20L, shop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponseDTO result = serviceService.toggleAvailability(5L, 20L, 1L);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void toggleAvailability_serviceFromOtherShop_throwsNotFound() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        BarberShop otherShop = shop(6L, owner);
        com.two_m.yourbarber.model.Service service = offering(20L, otherShop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));

        assertThrows(
                ResourceNotFoundException.class,
                () -> serviceService.toggleAvailability(5L, 20L, 1L));
    }

    @Test
    void deleteService_owner_deletes() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        com.two_m.yourbarber.model.Service service = offering(20L, shop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));

        serviceService.deleteService(5L, 20L, 1L);

        verify(serviceRepository).delete(service);
    }

    @Test
    void toggleAvailability_assignedBarber_flipsFlag() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        Barber member = member(2L, shop);
        com.two_m.yourbarber.model.Service service = offering(20L, shop, member);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponseDTO result = serviceService.toggleAvailability(5L, 20L, 2L);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void deleteService_unrelatedBarber_throwsForbidden() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        Barber member = member(2L, shop);
        Barber otherMember = member(3L, shop);
        com.two_m.yourbarber.model.Service service = offering(20L, shop, member);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));

        assertThrows(
                ForbiddenOperationException.class,
                () -> serviceService.deleteService(5L, 20L, otherMember.getId()));
    }
}
