package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.ClientBlock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientBlockRepository extends JpaRepository<ClientBlock, Long> {

    List<ClientBlock> findByBarberIdOrderByCreatedAtDesc(Long barberId);

    boolean existsByBarberIdAndClientId(Long barberId, Long clientId);

    Optional<ClientBlock> findByBarberIdAndClientId(Long barberId, Long clientId);
}
