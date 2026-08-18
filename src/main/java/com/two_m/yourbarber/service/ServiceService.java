package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.service.ServicePostPutDTO;
import com.two_m.yourbarber.dto.service.ServiceResponseDTO;
import java.util.List;

public interface ServiceService {

    ServiceResponseDTO createService(Long shopId, ServicePostPutDTO dto, Long requesterId);

    List<ServiceResponseDTO> listServices(Long shopId);

    ServiceResponseDTO updateService(
            Long shopId, Long serviceId, ServicePostPutDTO dto, Long requesterId);

    void deleteService(Long shopId, Long serviceId, Long requesterId);

    ServiceResponseDTO toggleAvailability(Long shopId, Long serviceId, Long requesterId);
}
