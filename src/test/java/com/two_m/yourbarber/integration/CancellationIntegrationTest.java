package com.two_m.yourbarber.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Who may cancel, when it's too late, and what a cancellation frees up. */
class CancellationIntegrationTest extends IntegrationTestBase {

    private record Booked(Actor owner, Actor client, long serviceId, long appointmentId, LocalDateTime slot) {}

    private Booked bookOne() throws Exception {
        Actor owner = registerShopOwner();
        long shopId = shopIdOf(owner);
        long serviceId = createService(owner, shopId, 30);
        Actor client = registerClient();
        LocalDateTime slot = nextMondayAt(10, 0);
        long id =
                body(
                                post(
                                        "/api/appointments",
                                        client.token(),
                                        bookingPayload(owner.id(), serviceId, slot),
                                        201))
                        .get("id")
                        .asLong();
        return new Booked(owner, client, serviceId, id, slot);
    }

    private String statusOf(Actor viewer, long appointmentId) throws Exception {
        for (JsonNode a : body(get("/api/appointments", viewer.token(), 200))) {
            if (a.get("id").asLong() == appointmentId) {
                return a.get("status").asText();
            }
        }
        throw new AssertionError("appointment " + appointmentId + " not visible to " + viewer.email());
    }

    @Test
    void clientCancelsOwnAppointment() throws Exception {
        Booked b = bookOne();

        patch("/api/appointments/" + b.appointmentId() + "/cancel", b.client().token(), null, 204);

        assertThat(statusOf(b.client(), b.appointmentId())).isEqualTo("CANCELLED");
    }

    @Test
    void barberCancelsAppointment() throws Exception {
        Booked b = bookOne();

        patch("/api/appointments/" + b.appointmentId() + "/cancel", b.owner().token(), null, 204);

        assertThat(statusOf(b.owner(), b.appointmentId())).isEqualTo("CANCELLED");
    }

    @Test
    void anUnrelatedClientCannotCancelSomeoneElsesAppointment() throws Exception {
        Booked b = bookOne();
        Actor stranger = registerClient();

        patch("/api/appointments/" + b.appointmentId() + "/cancel", stranger.token(), null, 403);

        assertThat(statusOf(b.client(), b.appointmentId())).isEqualTo("PENDING");
    }

    @Test
    void aCompletedAppointmentCanNoLongerBeCancelled() throws Exception {
        Booked b = bookOne();
        patch(
                "/api/appointments/" + b.appointmentId() + "/status",
                b.owner().token(),
                Map.of("status", "CONFIRMED"),
                200);
        patch(
                "/api/appointments/" + b.appointmentId() + "/status",
                b.owner().token(),
                Map.of("status", "COMPLETED"),
                200);

        String message =
                message(
                        patch(
                                "/api/appointments/" + b.appointmentId() + "/cancel",
                                b.client().token(),
                                null,
                                400));

        assertThat(message).containsIgnoringCase("não pode mais ser cancelado");
    }

    @Test
    void cancellingTwiceIsRejected() throws Exception {
        Booked b = bookOne();
        patch("/api/appointments/" + b.appointmentId() + "/cancel", b.client().token(), null, 204);

        patch("/api/appointments/" + b.appointmentId() + "/cancel", b.client().token(), null, 400);
    }

    @Test
    void cancellingFreesTheSlotForAnotherClient() throws Exception {
        Booked b = bookOne();

        // The slot is taken — a second client is turned away.
        post(
                "/api/appointments",
                registerClient().token(),
                bookingPayload(b.owner().id(), b.serviceId(), b.slot()),
                400);

        patch("/api/appointments/" + b.appointmentId() + "/cancel", b.client().token(), null, 204);

        // Now it goes through.
        post(
                "/api/appointments",
                registerClient().token(),
                bookingPayload(b.owner().id(), b.serviceId(), b.slot()),
                201);
    }
}
