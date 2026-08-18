package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import java.util.List;

public interface AdminService {

    List<BarberShopRequestResponseDTO> listPendingRequests();

    BarberShopRequestResponseDTO decideRequest(Long requestId, boolean approved);
}
