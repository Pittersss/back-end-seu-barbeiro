package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.appointment.AppointmentResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.user.UserProfileDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    List<BarberShopRequestResponseDTO> listPendingRequests();

    BarberShopRequestResponseDTO decideRequest(Long requestId, boolean approved);

    List<UserProfileDTO> listClients();

    void deleteClient(Long clientId);

    void deleteBarberShop(Long shopId);

    Page<AppointmentResponseDTO> listAppointments(Pageable pageable);
}
