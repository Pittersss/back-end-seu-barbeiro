package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.SubscriptionPayment;
import com.two_m.yourbarber.model.enums.SubscriptionPaymentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    List<SubscriptionPayment> findByBarberIdOrderByCreatedAtDesc(Long barberId);

    List<SubscriptionPayment> findByStatus(SubscriptionPaymentStatus status);
}
