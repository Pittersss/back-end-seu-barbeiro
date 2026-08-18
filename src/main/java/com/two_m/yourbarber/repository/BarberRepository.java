package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.Barber;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long> {

    Optional<Barber> findByEmail(String email);
}
