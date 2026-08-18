package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.JoinRequest;
import com.two_m.yourbarber.model.enums.RequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByBarberShopIdAndStatus(Long barberShopId, RequestStatus status);

    List<JoinRequest> findByBarberId(Long barberId);
}
