package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.service.ServicePostPutDTO;
import com.two_m.yourbarber.dto.service.ServiceResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.ServiceMapper;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.ServiceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final BarberShopRepository barberShopRepository;

    @Override
    public ServiceResponseDTO createService(
            Long shopId, ServicePostPutDTO dto, Long requesterId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);

        com.two_m.yourbarber.model.Service service =
                com.two_m.yourbarber.model.Service.builder()
                        .name(dto.getName())
                        .description(dto.getDescription())
                        .durationMinutes(dto.getDurationMinutes())
                        .price(dto.getPrice())
                        .image(dto.getImage())
                        .barberShop(shop)
                        .build();

        return ServiceMapper.toDto(serviceRepository.save(service));
    }

    @Override
    public List<ServiceResponseDTO> listServices(Long shopId) {
        findShop(shopId);
        return serviceRepository.findByBarberShopId(shopId).stream()
                .map(ServiceMapper::toDto)
                .toList();
    }

    @Override
    public ServiceResponseDTO updateService(
            Long shopId, Long serviceId, ServicePostPutDTO dto, Long requesterId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        com.two_m.yourbarber.model.Service service = findServiceInShop(shop, serviceId);

        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setDurationMinutes(dto.getDurationMinutes());
        service.setPrice(dto.getPrice());
        service.setImage(dto.getImage());

        return ServiceMapper.toDto(serviceRepository.save(service));
    }

    @Override
    public void deleteService(Long shopId, Long serviceId, Long requesterId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        com.two_m.yourbarber.model.Service service = findServiceInShop(shop, serviceId);
        serviceRepository.delete(service);
    }

    @Override
    public ServiceResponseDTO toggleAvailability(
            Long shopId, Long serviceId, Long requesterId) {
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        com.two_m.yourbarber.model.Service service = findServiceInShop(shop, serviceId);

        service.setAvailable(!service.isAvailable());
        return ServiceMapper.toDto(serviceRepository.save(service));
    }

    private com.two_m.yourbarber.model.Service findServiceInShop(
            BarberShop shop, Long serviceId) {
        com.two_m.yourbarber.model.Service service =
                serviceRepository
                        .findById(serviceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Service not found: " + serviceId));
        if (service.getBarberShop() == null
                || !service.getBarberShop().getId().equals(shop.getId())) {
            throw new ResourceNotFoundException(
                    "Service " + serviceId + " does not belong to shop " + shop.getId());
        }
        return service;
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
}
