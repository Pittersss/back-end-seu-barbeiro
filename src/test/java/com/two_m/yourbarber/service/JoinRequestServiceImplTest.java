package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.joinrequest.JoinRequestDecisionDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestPostDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.JoinRequest;
import com.two_m.yourbarber.model.enums.RequestStatus;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.JoinRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private BarberShopRepository barberShopRepository;
    @Mock private BarberRepository barberRepository;

    @InjectMocks private JoinRequestServiceImpl joinRequestService;

    private Barber barber(long id) {
        Barber barber =
                Barber.builder()
                        .name("Barber")
                        .email("barber@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .build();
        barber.setId(id);
        return barber;
    }

    private BarberShop shop(long id, Barber owner, boolean accepting) {
        BarberShop shop =
                BarberShop.builder().name("Shop").owner(owner).acceptingBarbers(accepting).build();
        shop.setId(id);
        return shop;
    }

    @Test
    void requestToJoin_barberWithoutShopAndShopAccepting_createsRequest() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, true);
        Barber applicant = barber(2L);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(applicant));
        when(joinRequestRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            JoinRequest request = inv.getArgument(0);
                            request.setId(50L);
                            request.setCreatedAt(java.time.LocalDateTime.now());
                            return request;
                        });

        JoinRequestPostDTO dto = new JoinRequestPostDTO("Please let me join");
        JoinRequestResponseDTO result = joinRequestService.requestToJoin(5L, dto, 2L);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getBarberId()).isEqualTo(2L);
    }

    @Test
    void requestToJoin_barberAlreadyInShop_throws() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, true);
        Barber applicant = barber(2L);
        applicant.setBarberShop(shop(6L, owner, true));
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(applicant));

        JoinRequestPostDTO dto = new JoinRequestPostDTO("Please let me join");

        assertThrows(
                BusinessRuleException.class,
                () -> joinRequestService.requestToJoin(5L, dto, 2L));
    }

    @Test
    void requestToJoin_shopNotAccepting_throws() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, false);
        Barber applicant = barber(2L);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(barberRepository.findById(2L)).thenReturn(Optional.of(applicant));

        JoinRequestPostDTO dto = new JoinRequestPostDTO("Please let me join");

        assertThrows(
                BusinessRuleException.class,
                () -> joinRequestService.requestToJoin(5L, dto, 2L));
    }

    @Test
    void listRequests_notOwner_throwsForbidden() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, true);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        assertThrows(
                ForbiddenOperationException.class,
                () -> joinRequestService.listRequests(5L, 2L));
    }

    @Test
    void decideRequest_accepted_assignsBarberToShop() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, true);
        Barber applicant = barber(2L);
        JoinRequest request =
                JoinRequest.builder().barber(applicant).barberShop(shop).build();
        request.setId(50L);

        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));
        when(joinRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JoinRequestDecisionDTO decision = new JoinRequestDecisionDTO(true);
        JoinRequestResponseDTO result =
                joinRequestService.decideRequest(5L, 50L, decision, 1L);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(applicant.getBarberShop()).isEqualTo(shop);
        verify(barberRepository).save(applicant);
    }

    @Test
    void decideRequest_rejected_doesNotAssignBarber() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, true);
        Barber applicant = barber(2L);
        JoinRequest request =
                JoinRequest.builder().barber(applicant).barberShop(shop).build();
        request.setId(50L);

        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));
        when(joinRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JoinRequestDecisionDTO decision = new JoinRequestDecisionDTO(false);
        JoinRequestResponseDTO result =
                joinRequestService.decideRequest(5L, 50L, decision, 1L);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED.name());
        assertThat(applicant.getBarberShop()).isNull();
        verify(barberRepository, never()).save(any());
    }

    @Test
    void decideRequest_requestFromOtherShop_throwsNotFound() {
        Barber owner = barber(1L);
        BarberShop shop = shop(5L, owner, true);
        BarberShop otherShop = shop(6L, owner, true);
        Barber applicant = barber(2L);
        JoinRequest request =
                JoinRequest.builder().barber(applicant).barberShop(otherShop).build();
        request.setId(50L);

        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));

        JoinRequestDecisionDTO decision = new JoinRequestDecisionDTO(true);

        assertThrows(
                ResourceNotFoundException.class,
                () -> joinRequestService.decideRequest(5L, 50L, decision, 1L));
    }
}
