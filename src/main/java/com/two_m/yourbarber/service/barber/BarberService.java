package com.two_m.yourbarber.service.barber;

import com.two_m.yourbarber.dto.barber.BarberPostPutDTO;
import com.two_m.yourbarber.dto.barber.BarberResponseDTO;

public interface BarberService {

    BarberResponseDTO getBarber(Long id);

    BarberResponseDTO updateBarber(Long id, BarberPostPutDTO dto, Long requesterId);

    void deleteBarber(Long id, Long requesterId);
}
