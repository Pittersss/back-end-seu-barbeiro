package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.BarberShopRequest;
import com.two_m.yourbarber.model.enums.RequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarberShopRequestRepository extends JpaRepository<BarberShopRequest, Long> {

    List<BarberShopRequest> findByStatus(RequestStatus status);
}
