package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.PushSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserId(Long userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    Optional<PushSubscription> findByExpoPushToken(String expoPushToken);

    void deleteByEndpoint(String endpoint);

    void deleteByExpoPushToken(String expoPushToken);
}
