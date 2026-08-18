package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.Service;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBarberShopId(Long barberShopId);
}
