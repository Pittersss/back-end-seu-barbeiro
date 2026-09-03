package com.two_m.yourbarber.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The booking endpoint is where the barber's schedule rules, the shop wiring and
 * the client blocklist all meet. These cover the paths a real client could
 * plausibly hit, not just the happy one.
 */
class BookingRulesIntegrationTest extends IntegrationTestBase {

    private record Shop(Actor owner, long shopId, long serviceId) {}

    private Shop shopWith30MinService(int workStart, int workEnd, Integer breakStart, Integer breakEnd)
            throws Exception {
        Actor owner = registerShopOwner();
        long shopId = shopIdOf(owner);
        long serviceId = createService(owner, shopId, 30);
        setWorkingHours(owner, workStart, workEnd, breakStart, breakEnd);
        return new Shop(owner, shopId, serviceId);
    }

    @Test
    void clientBooksInsideWindow_isCreatedPending() throws Exception {
        Shop shop = shopWith30MinService(9, 18, null, null);
        Actor client = registerClient();

        JsonNode created =
                body(
                        post(
                                "/api/appointments",
                                client.token(),
                                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(10, 0)),
                                201));

        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        assertThat(created.get("clientId").asLong()).isEqualTo(client.id());
        assertThat(created.get("barberId").asLong()).isEqualTo(shop.owner().id());
    }

    @Test
    void secondClientCannotTakeAnAlreadyBookedSlot() throws Exception {
        Shop shop = shopWith30MinService(9, 18, null, null);
        LocalDateTime slot = nextMondayAt(10, 0);
        post(
                "/api/appointments",
                registerClient().token(),
                bookingPayload(shop.owner().id(), shop.serviceId(), slot),
                201);

        String message =
                message(
                        post(
                                "/api/appointments",
                                registerClient().token(),
                                bookingPayload(shop.owner().id(), shop.serviceId(), slot),
                                400));

        assertThat(message).containsIgnoringCase("appointment at that time");
    }

    @Test
    void overlappingButNotIdenticalSlotIsRejected() throws Exception {
        Shop shop = shopWith30MinService(9, 18, null, null);
        post(
                "/api/appointments",
                registerClient().token(),
                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(10, 0)),
                201);

        // 10:15–10:45 overlaps the existing 10:00–10:30 booking.
        post(
                "/api/appointments",
                registerClient().token(),
                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(10, 15)),
                400);
    }

    @Test
    void bookingOutsideTheWorkingWindowIsRejected() throws Exception {
        Shop shop = shopWith30MinService(9, 12, null, null);

        String message =
                message(
                        post(
                                "/api/appointments",
                                registerClient().token(),
                                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(13, 0)),
                                400));

        assertThat(message).contains("Fora do horário");
    }

    @Test
    void bookingThatSpillsPastClosingIsRejected() throws Exception {
        Shop shop = shopWith30MinService(9, 12, null, null);

        // 11:45 + 30min would end at 12:15, past the 12:00 close.
        post(
                "/api/appointments",
                registerClient().token(),
                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(11, 45)),
                400);
    }

    @Test
    void bookingDuringTheDailyBreakIsRejected() throws Exception {
        Shop shop = shopWith30MinService(9, 18, 12, 13);

        String message =
                message(
                        post(
                                "/api/appointments",
                                registerClient().token(),
                                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(12, 0)),
                                400));

        assertThat(message).contains("intervalo");
    }

    @Test
    void bookingInsideAOneOffTimeBlockIsRejected() throws Exception {
        Shop shop = shopWith30MinService(9, 18, null, null);
        LocalDateTime blockStart = nextMondayAt(10, 0);
        post(
                "/api/barbers/" + shop.owner().id() + "/time-blocks",
                shop.owner().token(),
                Map.of(
                        "startsAt", blockStart.toString(),
                        "endsAt", blockStart.plusHours(2).toString(),
                        "reason", "Consulta"),
                201);

        String message =
                message(
                        post(
                                "/api/appointments",
                                registerClient().token(),
                                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(11, 0)),
                                400));

        assertThat(message).contains("bloqueou");
    }

    @Test
    void blockedClientCannotBookButCanAfterBeingUnblocked() throws Exception {
        Shop shop = shopWith30MinService(9, 18, null, null);
        Actor client = registerClient();

        post(
                "/api/barbers/" + shop.owner().id() + "/blocked-clients",
                shop.owner().token(),
                Map.of("clientId", client.id(), "reason", "faltou"),
                201);

        post(
                "/api/appointments",
                client.token(),
                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(10, 0)),
                403);

        delete(
                "/api/barbers/" + shop.owner().id() + "/blocked-clients/" + client.id(),
                shop.owner().token(),
                204);

        post(
                "/api/appointments",
                client.token(),
                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(10, 0)),
                201);
    }

    @Test
    void cannotBookAServiceThatBelongsToAnotherShop() throws Exception {
        Shop shopA = shopWith30MinService(9, 18, null, null);
        Shop shopB = shopWith30MinService(9, 18, null, null);

        String message =
                message(
                        post(
                                "/api/appointments",
                                registerClient().token(),
                                bookingPayload(shopA.owner().id(), shopB.serviceId(), nextMondayAt(10, 0)),
                                400));

        assertThat(message).containsIgnoringCase("does not offer this service");
    }

    @Test
    void cannotBookAnUnavailableBarber() throws Exception {
        Shop shop = shopWith30MinService(9, 18, null, null);
        patch("/api/barbers/" + shop.owner().id() + "/availability", shop.owner().token(), null, 200);

        String message =
                message(
                        post(
                                "/api/appointments",
                                registerClient().token(),
                                bookingPayload(shop.owner().id(), shop.serviceId(), nextMondayAt(10, 0)),
                                400));

        assertThat(message).containsIgnoringCase("not currently available");
    }

    @Test
    void openSlotsMatchTheRulesAndReactToBookingsAndBlocks() throws Exception {
        Shop shop = shopWith30MinService(9, 12, null, null);
        Actor client = registerClient();
        LocalDate day = nextMondayAt(9, 0).toLocalDate();
        String slotsUrl =
                "/api/barbers/"
                        + shop.owner().id()
                        + "/open-slots?serviceId="
                        + shop.serviceId()
                        + "&from="
                        + day
                        + "&to="
                        + day;

        JsonNode slots = body(get(slotsUrl, client.token(), 200)).get(day.toString());
        assertThat(slots).hasSize(6); // 09:00, 09:30, 10:00, 10:30, 11:00, 11:30

        // A slot the endpoint offered must actually be bookable.
        String offered = slots.get(0).asText();
        post(
                "/api/appointments",
                client.token(),
                Map.of(
                        "barberId", shop.owner().id(),
                        "serviceId", shop.serviceId(),
                        "scheduledAt", offered,
                        "paymentMethod", "CASH"),
                201);

        assertThat(body(get(slotsUrl, client.token(), 200)).get(day.toString())).hasSize(5);

        // A time block covering the rest of the morning empties the day.
        post(
                "/api/barbers/" + shop.owner().id() + "/time-blocks",
                shop.owner().token(),
                Map.of(
                        "startsAt", day.atTime(9, 0).toString(),
                        "endsAt", day.atTime(12, 0).toString()),
                201);
        assertThat(body(get(slotsUrl, client.token(), 200)).has(day.toString())).isFalse();
    }
}
