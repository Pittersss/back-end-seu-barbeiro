package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.TimeBlock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {

    List<TimeBlock> findByBarberId(Long barberId);

    List<TimeBlock> findByBarberIdAndEndsAtAfterOrderByStartsAt(
            Long barberId, LocalDateTime after);
}
