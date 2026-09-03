package com.two_m.yourbarber.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Authorization boundaries between the three roles, plus the full "barber becomes
 * a shop owner and a client books there" lifecycle.
 */
class RoleAndOwnershipIntegrationTest extends IntegrationTestBase {

    @Test
    void aBarberCannotEditAnotherBarbersProfile() throws Exception {
        Actor a = registerBarber();
        Actor b = registerBarber();

        Map<String, Object> payload =
                Map.of(
                        "name", "hijacked",
                        "delayTolerance", 0,
                        "workStartHour", 8,
                        "workEndHour", 20);
        patch("/api/barbers/" + b.id() + "/availability", a.token(), null, 403);
        put("/api/barbers/" + b.id(), a.token(), payload, 403);
    }

    @Test
    void aBarberCannotDeleteAnotherBarbersTimeBlock() throws Exception {
        Actor owner = registerBarber();
        LocalDateTime start = nextMondayAt(10, 0);
        long blockId =
                body(
                                post(
                                        "/api/barbers/" + owner.id() + "/time-blocks",
                                        owner.token(),
                                        Map.of(
                                                "startsAt", start.toString(),
                                                "endsAt", start.plusHours(1).toString()),
                                        201))
                        .get("id")
                        .asLong();

        Actor intruder = registerBarber();
        delete("/api/barbers/" + owner.id() + "/time-blocks/" + blockId, intruder.token(), 403);
    }

    @Test
    void aClientCannotReadABarbersBlocklist() throws Exception {
        Actor barber = registerBarber();
        Actor client = registerClient();

        get("/api/barbers/" + barber.id() + "/blocked-clients", client.token(), 403);
    }

    @Test
    void aClientCannotReachAdminEndpoints() throws Exception {
        Actor client = registerClient();

        get("/api/admin/barbershop-requests", client.token(), 403);
        patch("/api/admin/barbershop-requests/1", client.token(), Map.of("approved", true), 403);
    }

    @Test
    void anUnverifiedAccountIsRejectedEvenWithAValidToken() throws Exception {
        String email = uniqueEmail("unverified");
        String token =
                body(
                                post(
                                        "/api/auth/register/client",
                                        null,
                                        Map.of("name", "u", "email", email, "password", "password123"),
                                        201))
                        .get("token")
                        .asText();

        // The token is well-formed, but the account is not e-mail-verified yet.
        get("/api/appointments", token, 401);
    }

    @Test
    void onlyTheAssignedBarberCanAdvanceAnAppointmentStatus() throws Exception {
        Actor owner = registerShopOwner();
        long shopId = shopIdOf(owner);
        long serviceId = createService(owner, shopId, 30);
        Actor client = registerClient();
        long appointmentId =
                body(
                                post(
                                        "/api/appointments",
                                        client.token(),
                                        bookingPayload(owner.id(), serviceId, nextMondayAt(10, 0)),
                                        201))
                        .get("id")
                        .asLong();

        // The client cannot confirm their own appointment.
        patch(
                "/api/appointments/" + appointmentId + "/status",
                client.token(),
                Map.of("status", "CONFIRMED"),
                403);

        // Neither can an unrelated barber.
        patch(
                "/api/appointments/" + appointmentId + "/status",
                registerBarber().token(),
                Map.of("status", "CONFIRMED"),
                403);

        patch(
                "/api/appointments/" + appointmentId + "/status",
                owner.token(),
                Map.of("status", "CONFIRMED"),
                200);
    }

    @Test
    void aNonOwnerBarberCannotManageAShop() throws Exception {
        Actor owner = registerShopOwner();
        long shopId = shopIdOf(owner);
        Actor other = registerBarber();

        put(
                "/api/barbershops/" + shopId,
                other.token(),
                Map.of("name", "renamed"),
                403);
        patch("/api/barbershops/" + shopId + "/accepting-barbers", other.token(), null, 403);
    }

    @Test
    void barberBecomesOwnerOnApprovalAndAClientCanThenBookThere() throws Exception {
        Actor barber = registerBarber();

        // Fresh barber: no shop yet.
        assertThat(body(get("/api/barbers/" + barber.id(), barber.token(), 200)).get("barberShopId").isNull())
                .isTrue();

        post(
                "/api/barbershops",
                barber.token(),
                Map.of("shopName", "Nova Barbearia", "shopAddress", "Centro"),
                201);

        // A second barber's request must not be the one the admin approves here.
        registerBarber();
        JsonNode pending = body(get("/api/admin/barbershop-requests", adminToken(), 200));
        long requestId = -1;
        for (JsonNode request : pending) {
            if (request.get("requesterId").asLong() == barber.id()) {
                requestId = request.get("id").asLong();
            }
        }
        assertThat(requestId).isPositive();

        patch(
                "/api/admin/barbershop-requests/" + requestId,
                adminToken(),
                Map.of("approved", true),
                200);

        long shopId =
                body(get("/api/barbers/" + barber.id(), barber.token(), 200)).get("barberShopId").asLong();
        JsonNode shop = body(get("/api/barbershops/" + shopId, barber.token(), 200));
        assertThat(shop.get("ownerId").asLong()).isEqualTo(barber.id());

        // The shop is now publicly listed and bookable.
        Actor client = registerClient();
        assertThat(body(get("/api/barbershops", client.token(), 200)).size()).isEqualTo(1);
        long serviceId = createService(barber, shopId, 30);
        post(
                "/api/appointments",
                client.token(),
                bookingPayload(barber.id(), serviceId, nextMondayAt(11, 0)),
                201);
    }

    @Test
    void aSecondShopRequestFromAnAlreadyOwningBarberIsRejected() throws Exception {
        Actor owner = registerShopOwner();

        String message =
                message(
                        post(
                                "/api/barbershops",
                                owner.token(),
                                Map.of("shopName", "Segunda"),
                                400));

        assertThat(message).containsIgnoringCase("already belongs to a barbershop");
    }
}
