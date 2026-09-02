package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.BarberShopMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.BarberShopRequest;
import com.two_m.yourbarber.model.enums.RequestStatus;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.BarberShopRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final BarberShopRequestRepository barberShopRequestRepository;
    private final BarberShopRepository barberShopRepository;
    private final BarberRepository barberRepository;

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

        return BarberShopMapper.toRequestDto(barberShopRequestRepository.save(request));
    }
}
