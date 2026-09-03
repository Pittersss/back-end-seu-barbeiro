package com.two_m.yourbarber.model.enums;

/** Computed, not persisted -- derived live from a barber's {@link SubscriptionPaymentStatus} history. */
public enum SubscriptionStatus {
    ACTIVE,
    PENDING_CONFIRMATION,
    INACTIVE
}
