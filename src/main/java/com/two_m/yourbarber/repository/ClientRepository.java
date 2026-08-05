package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
