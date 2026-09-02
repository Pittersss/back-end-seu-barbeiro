package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopPostPutDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopResponseDTO;
import java.util.List;

public interface BarberShopService {

    BarberShopRequestResponseDTO requestCreation(BarberShopRequestDTO dto, Long ownerId);

    List<BarberShopResponseDTO> listBarberShops();

    BarberShopResponseDTO getBarberShop(Long id);

    BarberShopResponseDTO updateBarberShop(
            Long id, BarberShopPostPutDTO dto, Long requesterId);

    void deleteBarberShop(Long id, Long requesterId);

    BarberShopResponseDTO toggleAcceptingBarbers(Long id, Long requesterId);

    List<BarberResponseDTO> listBarbers(Long shopId);

    BarberResponseDTO toggleBarberAvailability(Long shopId, Long barberId, Long requesterId);

    void removeBarberFromShop(Long shopId, Long barberId, Long requesterId);
}
