package com.two_m.yourbarber.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.two_m.yourbarber.model.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Input-validation edges on the schedule endpoints, plus the e-mail verification handshake. */
class AvailabilityAndAuthIntegrationTest extends IntegrationTestBase {

    @Test
    void workingHoursMustBeConsistent() throws Exception {
        Actor barber = registerBarber();

        Map<String, Object> reversed = new HashMap<>();
        reversed.put("name", "b");
        reversed.put("delayTolerance", 0);
        reversed.put("workStartHour", 19);
        reversed.put("workEndHour", 9);
        assertThat(message(put("/api/barbers/" + barber.id(), barber.token(), reversed, 400)))
                .contains("abertura");

        Map<String, Object> breakOutside = new HashMap<>();
        breakOutside.put("name", "b");
        breakOutside.put("delayTolerance", 0);
        breakOutside.put("workStartHour", 9);
        breakOutside.put("workEndHour", 18);
        breakOutside.put("breakStartHour", 8);
        breakOutside.put("breakEndHour", 10);
        assertThat(message(put("/api/barbers/" + barber.id(), barber.token(), breakOutside, 400)))
                .contains("intervalo");
    }

    @Test
    void toggleAvailabilityFlipsTheFlag() throws Exception {
        Actor barber = registerBarber();
        assertThat(body(get("/api/barbers/" + barber.id(), barber.token(), 200)).get("available").asBoolean())
                .isTrue();

        patch("/api/barbers/" + barber.id() + "/availability", barber.token(), null, 200);

        assertThat(body(get("/api/barbers/" + barber.id(), barber.token(), 200)).get("available").asBoolean())
                .isFalse();
    }

    @Test
    void timeBlockRangeIsValidated() throws Exception {
        Actor barber = registerBarber();
        LocalDateTime monday = nextMondayAt(10, 0);

        // end before start
        post(
                "/api/barbers/" + barber.id() + "/time-blocks",
                barber.token(),
                Map.of("startsAt", monday.toString(), "endsAt", monday.minusHours(1).toString()),
                400);

        // entirely in the past
        post(
                "/api/barbers/" + barber.id() + "/time-blocks",
                barber.token(),
                Map.of(
                        "startsAt", LocalDateTime.now().minusDays(2).toString(),
                        "endsAt", LocalDateTime.now().minusDays(1).toString()),
                400);
    }

    @Test
    void emailVerificationHandshake() throws Exception {
        String email = uniqueEmail("verify");
        long id =
                body(
                                post(
                                        "/api/auth/register/client",
                                        null,
                                        Map.of("name", "v", "email", email, "password", "password123"),
                                        201))
                        .get("userId")
                        .asLong();

        // Login is blocked until the address is confirmed.
        post(
                "/api/auth/login",
                null,
                Map.of("email", email, "password", "password123"),
                403);

        // A wrong code is refused.
        post(
                "/api/auth/verify-email",
                null,
                Map.of("email", email, "code", "000000"),
                400);

        String realCode = userRepository.findById(id).map(User::getVerificationCode).orElseThrow();
        post("/api/auth/verify-email", null, Map.of("email", email, "code", realCode), 200);

        // Now it works.
        post(
                "/api/auth/login",
                null,
                Map.of("email", email, "password", "password123"),
                200);
    }
}
