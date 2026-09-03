package com.two_m.yourbarber.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * End-to-end: a freshly registered barber has no active subscription and is blocked from
 * managing their shop, all the way through requesting the Pix charge and having an admin
 * confirm it.
 */
class SubscriptionIntegrationTest extends IntegrationTestBase {

    private Actor unsubscribedShopOwner() throws Exception {
        Actor owner = registerUnsubscribedBarber();
        post(
                "/api/barbershops",
                owner.token(),
                Map.of("shopName", "Shop " + owner.id(), "shopAddress", "Rua 1"),
                201);
        JsonNode pending = body(get("/api/admin/barbershop-requests", adminToken(), 200));
        long requestId = -1;
        for (JsonNode request : pending) {
            if (request.get("requesterId").asLong() == owner.id()) {
                requestId = request.get("id").asLong();
            }
        }
        patch(
                "/api/admin/barbershop-requests/" + requestId,
                adminToken(),
                Map.of("approved", true),
                200);
        return owner;
    }

    @Test
    void statusStartsInactiveAndBlocksManagement() throws Exception {
        Actor owner = unsubscribedShopOwner();
        long shopId = shopIdOf(owner);

        assertThat(body(get("/api/subscriptions/me", owner.token(), 200)).get("status").asText())
                .isEqualTo("INACTIVE");

        Map<String, Object> service =
                Map.of("name", "Corte", "durationMinutes", 30, "price", new BigDecimal("50.00"));
        String message =
                message(
                        post(
                                "/api/barbershops/" + shopId + "/services",
                                owner.token(),
                                service,
                                402));
        assertThat(message).containsIgnoringCase("assinatura");
    }

    @Test
    void payingAndBeingConfirmedUnlocksManagement() throws Exception {
        Actor owner = unsubscribedShopOwner();
        long shopId = shopIdOf(owner);

        JsonNode pix = body(post("/api/subscriptions/me/pix", owner.token(), null, 200));
        assertThat(pix.get("pixCopyPaste").asText()).isNotBlank();
        assertThat(pix.get("qrCodeBase64").asText()).isNotBlank();

        assertThat(body(get("/api/subscriptions/me", owner.token(), 200)).get("status").asText())
                .isEqualTo("PENDING_CONFIRMATION");

        JsonNode pendingPayments = body(get("/api/admin/subscription-payments", adminToken(), 200));
        long paymentId = -1;
        for (JsonNode payment : pendingPayments) {
            if (payment.get("barberId").asLong() == owner.id()) {
                paymentId = payment.get("id").asLong();
            }
        }
        assertThat(paymentId).isNotEqualTo(-1);

        patch(
                "/api/admin/subscription-payments/" + paymentId,
                adminToken(),
                Map.of("approved", true),
                200);

        JsonNode status = body(get("/api/subscriptions/me", owner.token(), 200));
        assertThat(status.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(status.get("periodEnd").asText()).isNotBlank();

        Map<String, Object> service =
                Map.of("name", "Corte", "durationMinutes", 30, "price", new BigDecimal("50.00"));
        post("/api/barbershops/" + shopId + "/services", owner.token(), service, 201);
    }
}
