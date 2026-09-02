package com.two_m.yourbarber.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AvailabilityService {

    /**
     * Bookable start times for a barber + service, day by day, within {@code [from, to]}.
     * Every returned slot fits the full service duration inside the barber's working
     * window, avoids the daily break, every one-off time block and every existing
     * appointment, and is in the future. Days with no openings are omitted.
     */
    Map<LocalDate, List<LocalDateTime>> openSlots(
            Long barberId, Long serviceId, LocalDate from, LocalDate to);
}
