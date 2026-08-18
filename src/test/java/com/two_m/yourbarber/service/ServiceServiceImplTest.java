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

    private BarberShop shop(long id, Barber owner) {
        BarberShop shop = BarberShop.builder().name("Shop").owner(owner).build();
        shop.setId(id);
        return shop;
    }

    private com.two_m.yourbarber.model.Service offering(long id, BarberShop shop) {
        com.two_m.yourbarber.model.Service service =
                com.two_m.yourbarber.model.Service.builder()
                        .name("Haircut")
                        .durationMinutes(30)
                        .price(BigDecimal.valueOf(50))
                        .available(true)
                        .barberShop(shop)
                        .build();
        service.setId(id);
        return service;
    }

    @Test
    void createService_owner_savesService() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServicePostPutDTO dto =
                new ServicePostPutDTO("Haircut", "desc", 30, BigDecimal.valueOf(50), null);
        ServiceResponseDTO result = serviceService.createService(5L, dto, 1L);

        assertThat(result.getName()).isEqualTo("Haircut");
        assertThat(result.getBarberShopId()).isEqualTo(5L);
    }

    @Test
    void createService_notOwner_throwsForbidden() {
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
}
