package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.Appointment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByClientId(Long clientId);

    List<Appointment> findByBarberId(Long barberId);

    List<Appointment> findByBarberIdAndScheduledAtBetween(
            Long barberId, LocalDateTime start, LocalDateTime end);
}
