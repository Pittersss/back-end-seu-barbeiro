package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.BarberShop;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarberShopRepository extends JpaRepository<BarberShop, Long> {

    Optional<BarberShop> findByOwnerId(Long ownerId);
}
