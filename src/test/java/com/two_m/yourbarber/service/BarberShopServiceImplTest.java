package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopPostPutDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.BarberShopRequestRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarberShopServiceImplTest {

    @Mock private BarberShopRepository barberShopRepository;
    @Mock private BarberShopRequestRepository barberShopRequestRepository;
    @Mock private BarberRepository barberRepository;

    @InjectMocks private BarberShopServiceImpl barberShopService;

    private Barber barber(long id) {
        Barber barber =
                Barber.builder()
                        .name("Owner")
                        .email("owner@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .available(true)
                        .build();
        barber.setId(id);
        return barber;
    }

    private BarberShop shop(long id, Barber owner) {
        BarberShop shop =
                BarberShop.builder()
                        .name("Shop")
                        .address("Address")
                        .acceptingBarbers(true)
                        .owner(owner)
                        .build();
        shop.setId(id);
        return shop;
    }

    @Test
    void requestCreation_barberWithoutShop_createsRequest() {
        Barber requester = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(barberShopRequestRepository.save(any())).thenAnswer(inv -> {
            var request = inv.getArgument(0, com.two_m.yourbarber.model.BarberShopRequest.class);
            request.setId(10L);
            request.setCreatedAt(java.time.LocalDateTime.now());
            return request;
        });

        BarberShopRequestDTO dto = new BarberShopRequestDTO("New Shop", "Address", "111");
        BarberShopRequestResponseDTO result = barberShopService.requestCreation(dto, 1L);

        assertThat(result.getShopName()).isEqualTo("New Shop");
        assertThat(result.getRequesterId()).isEqualTo(1L);
    }

    @Test
    void requestCreation_barberAlreadyInShop_throws() {
        Barber requester = barber(1L);
        requester.setBarberShop(shop(5L, requester));
        when(barberRepository.findById(1L)).thenReturn(Optional.of(requester));

        BarberShopRequestDTO dto = new BarberShopRequestDTO("New Shop", "Address", "111");

        assertThrows(
                BusinessRuleException.class,
                () -> barberShopService.requestCreation(dto, 1L));
    }

    @Test
    void listBarberShops_returnsAllMapped() {
        Barber owner = barber(1L);
        BarberShop shopOne = shop(5L, owner);
        BarberShop shopTwo = shop(6L, owner);
        when(barberShopRepository.findAll()).thenReturn(List.of(shopOne, shopTwo));

        List<BarberShopResponseDTO> result = barberShopService.listBarberShops();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(5L);
        assertThat(result.get(1).getId()).isEqualTo(6L);
    }

    @Test
    void getBarberShop_found_returnsDto() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        BarberShopResponseDTO result = barberShopService.getBarberShop(5L);

        assertThat(result.getName()).isEqualTo("Shop");
        assertThat(result.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void getBarberShop_notFound_throws() {
        when(barberShopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> barberShopService.getBarberShop(99L));
    }

    @Test
    void updateBarberShop_owner_updatesFields() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberShopRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BarberShopPostPutDTO dto = new BarberShopPostPutDTO("Renamed", "New Addr", "222", null);
        BarberShopResponseDTO result = barberShopService.updateBarberShop(5L, dto, 1L);

        assertThat(result.getName()).isEqualTo("Renamed");
    }

    @Test
    void updateBarberShop_owner_updatesPhoto() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberShopRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BarberShopPostPutDTO dto = new BarberShopPostPutDTO("Shop", null, null, "base64photo");
        BarberShopResponseDTO result = barberShopService.updateBarberShop(5L, dto, 1L);

        assertThat(result.getPhotoBase64()).isEqualTo("base64photo");
    }

    @Test
    void updateBarberShop_notOwner_throwsForbidden() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        BarberShopPostPutDTO dto = new BarberShopPostPutDTO("Renamed", "New Addr", "222", null);

        assertThrows(
                ForbiddenOperationException.class,
                () -> barberShopService.updateBarberShop(5L, dto, 2L));
    }

    @Test
    void toggleAcceptingBarbers_owner_flipsFlag() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberShopRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BarberShopResponseDTO result = barberShopService.toggleAcceptingBarbers(5L, 1L);

        assertThat(result.isAcceptingBarbers()).isFalse();
    }

    @Test
    void listBarbers_returnsShopBarbers() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        Barber member = barber(2L);
        member.setBarberShop(shop);
        shop.setBarbers(List.of(member));
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        List<BarberResponseDTO> result = barberShopService.listBarbers(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void toggleBarberAvailability_memberBarber_flipsFlag() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        Barber member = barber(2L);
        member.setBarberShop(shop);
        member.setAvailable(true);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(barberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BarberResponseDTO result = barberShopService.toggleBarberAvailability(5L, 2L, 1L);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void toggleBarberAvailability_barberNotInShop_throwsBusinessRule() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        Barber outsider = barber(3L);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(3L)).thenReturn(Optional.of(outsider));

        assertThrows(
                BusinessRuleException.class,
                () -> barberShopService.toggleBarberAvailability(5L, 3L, 1L));
    }

    @Test
    void removeBarberFromShop_owner_clearsAssociation() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner);
        Barber member = barber(2L);
        member.setBarberShop(shop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(barberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        barberShopService.removeBarberFromShop(5L, 2L, 1L);

        assertThat(member.getBarberShop()).isNull();
    }
}
