package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.joinrequest.JoinRequestDecisionDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestPostDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.JoinRequestMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.JoinRequest;
import com.two_m.yourbarber.model.enums.RequestStatus;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.JoinRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JoinRequestServiceImpl implements JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final BarberShopRepository barberShopRepository;
    private final BarberRepository barberRepository;

    @Override
    public JoinRequestResponseDTO requestToJoin(
            Long shopId, JoinRequestPostDTO dto, Long barberId) {
        BarberShop shop = findShop(shopId);
        Barber barber = findBarber(barberId);

        if (barber.getBarberShop() != null) {
            throw new BusinessRuleException("Barber already belongs to a barbershop");
        }
        if (!shop.isAcceptingBarbers()) {
            throw new BusinessRuleException("This barbershop is not accepting new barbers");
        }

        JoinRequest request =
                JoinRequest.builder()
                        .message(dto.getMessage())
                        .barber(barber)
                        .barberShop(shop)
                        .build();

        return JoinRequestMapper.toDto(joinRequestRepository.save(request));
    }

    @Override
    public List<JoinRequestResponseDTO> listRequests(Long shopId, Long ownerId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, ownerId);

        return joinRequestRepository
                .findByBarberShopIdAndStatus(shopId, RequestStatus.PENDING)
                .stream()
                .map(JoinRequestMapper::toDto)
                .toList();
    }

    @Override
    public JoinRequestResponseDTO decideRequest(
            Long shopId, Long requestId, JoinRequestDecisionDTO decision, Long ownerId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, ownerId);
        JoinRequest request = findRequestInShop(shop, requestId);

        request.setStatus(decision.isAccepted() ? RequestStatus.APPROVED : RequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());

        if (decision.isAccepted()) {
            Barber barber = request.getBarber();
            barber.setBarberShop(shop);
            barberRepository.save(barber);
        }

        return JoinRequestMapper.toDto(joinRequestRepository.save(request));
    }

    private JoinRequest findRequestInShop(BarberShop shop, Long requestId) {
        JoinRequest request =
                joinRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Join request not found: " + requestId));
        if (!request.getBarberShop().getId().equals(shop.getId())) {
            throw new ResourceNotFoundException(
                    "Join request " + requestId + " does not belong to shop " + shop.getId());
        }
        return request;
    }

    private void assertOwner(BarberShop shop, Long ownerId) {
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenOperationException("Only the shop owner can perform this action");
        }
    }

    private BarberShop findShop(Long id) {
        return barberShopRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Barbershop not found: " + id));
    }

    private Barber findBarber(Long id) {
        return barberRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found: " + id));
    }
}
