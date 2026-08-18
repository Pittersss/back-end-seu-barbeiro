package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.BarberShopRequest;
import com.two_m.yourbarber.model.enums.RequestStatus;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.BarberShopRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private BarberShopRequestRepository barberShopRequestRepository;
    @Mock private BarberShopRepository barberShopRepository;
    @Mock private BarberRepository barberRepository;

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
}
