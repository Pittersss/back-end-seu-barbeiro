package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopPostPutDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.BarberMapper;
import com.two_m.yourbarber.mapper.BarberShopMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.BarberShopRequest;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.BarberShopRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BarberShopServiceImpl implements BarberShopService {

    private final BarberShopRepository barberShopRepository;
    private final BarberShopRequestRepository barberShopRequestRepository;
    private final BarberRepository barberRepository;

    @Override
    public BarberShopRequestResponseDTO requestCreation(
            BarberShopRequestDTO dto, Long ownerId) {
        Barber requester = findBarber(ownerId);
        if (requester.getBarberShop() != null) {
            throw new BusinessRuleException("Barber already belongs to a barbershop");
        }

        BarberShopRequest request =
                BarberShopRequest.builder()
                        .shopName(dto.getShopName())
                        .shopAddress(dto.getShopAddress())
                        .shopPhone(dto.getShopPhone())
                        .requester(requester)
                        .build();

        return BarberShopMapper.toRequestDto(barberShopRequestRepository.save(request));
    }

    @Override
    public List<BarberShopResponseDTO> listBarberShops() {
        return barberShopRepository.findAll().stream().map(BarberShopMapper::toDto).toList();
    }

    @Override
    public BarberShopResponseDTO getBarberShop(Long id) {
        return BarberShopMapper.toDto(findShop(id));
    }

    @Override
    public BarberShopResponseDTO updateBarberShop(
            Long id, BarberShopPostPutDTO dto, Long requesterId) {
        BarberShop shop = findShop(id);
        assertOwner(shop, requesterId);

        shop.setName(dto.getName());
        shop.setAddress(dto.getAddress());
        shop.setPhone(dto.getPhone());

        return BarberShopMapper.toDto(barberShopRepository.save(shop));
    }

    @Override
    public void deleteBarberShop(Long id, Long requesterId) {
        BarberShop shop = findShop(id);
        assertOwner(shop, requesterId);
        barberShopRepository.delete(shop);
    }

    @Override
    public BarberShopResponseDTO toggleAcceptingBarbers(Long id, Long requesterId) {
        BarberShop shop = findShop(id);
        assertOwner(shop, requesterId);
        shop.setAcceptingBarbers(!shop.isAcceptingBarbers());
        return BarberShopMapper.toDto(barberShopRepository.save(shop));
    }

    @Override
    public List<BarberResponseDTO> listBarbers(Long shopId) {
        return findShop(shopId).getBarbers().stream().map(BarberMapper::toDto).toList();
    }

    @Override
    public BarberResponseDTO toggleBarberAvailability(
            Long shopId, Long barberId, Long requesterId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        Barber barber = findMemberBarber(shop, barberId);

        barber.setAvailable(!barber.isAvailable());
        return BarberMapper.toDto(barberRepository.save(barber));
    }

    @Override
    public void removeBarberFromShop(Long shopId, Long barberId, Long requesterId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        Barber barber = findMemberBarber(shop, barberId);

        barber.setBarberShop(null);
        barberRepository.save(barber);
    }

    private Barber findMemberBarber(BarberShop shop, Long barberId) {
        Barber barber = findBarber(barberId);
        if (barber.getBarberShop() == null || !barber.getBarberShop().getId().equals(shop.getId())) {
            throw new BusinessRuleException("Barber does not belong to this barbershop");
        }
        return barber;
    }

    private void assertOwner(BarberShop shop, Long requesterId) {
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(requesterId)) {
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
