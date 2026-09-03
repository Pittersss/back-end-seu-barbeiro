package com.two_m.yourbarber.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared setup for the end-to-end controller tests: one Postgres container (Flyway
 * migrations run against it), MockMvc, a stubbed mail sender, and helpers for the
 * register → verify → authenticate dance plus the barber → owner → client fixture
 * graph.
 *
 * <p>Domain tables are wiped before every test (the seeded admin is kept), so the
 * assertions never depend on rows left behind by another test. Test transactions
 * are deliberately NOT used — the services call {@code setRollbackOnly()} on
 * expected 4xx paths, which would poison a shared test transaction for the
 * follow-up requests these scenarios make.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean protected JavaMailSender mailSender;

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    @Value("${admin.default-email:admin@yourbarber.com}")
    private String adminEmail;

    @Value("${admin.default-password:ChangeMe123!}")
    private String adminPassword;

    private String cachedAdminToken;

    @BeforeEach
    void wipeDomainTables() {
        // Ordered DELETEs rather than TRUNCATE CASCADE: every domain table
        // FK-references `users`, and `users` FK-references `barber_shops`, so a
        // cascading truncate would also wipe the seeded admin.
        jdbc.update("DELETE FROM appointments");
        jdbc.update("DELETE FROM time_blocks");
        jdbc.update("DELETE FROM client_blocks");
        jdbc.update("DELETE FROM services");
        jdbc.update("DELETE FROM join_requests");
        jdbc.update("DELETE FROM barbershop_requests");
        jdbc.update("DELETE FROM subscription_payments");
        jdbc.update("UPDATE users SET barber_shop_id = NULL");
        jdbc.update("DELETE FROM barber_shops");
        jdbc.update("DELETE FROM users WHERE role <> 'ADMIN'");
        cachedAdminToken = null;
    }

    protected String uniqueEmail(String prefix) {
        return prefix + COUNTER.incrementAndGet() + "@test.local";
    }

    // --- HTTP helpers -------------------------------------------------------

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected String message(MvcResult result) throws Exception {
        return body(result).path("message").asText();
    }

    protected MvcResult send(
            MockHttpServletRequestBuilder request, String token, Object payload, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (payload != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(payload));
        }
        MvcResult result = mvc.perform(request).andReturn();
        int actual = result.getResponse().getStatus();
        if (actual != expectedStatus) {
            throw new AssertionError(
                    "Expected HTTP "
                            + expectedStatus
                            + " but got "
                            + actual
                            + " for "
                            + result.getRequest().getMethod()
                            + " "
                            + result.getRequest().getRequestURI()
                            + " -> "
                            + result.getResponse().getContentAsString());
        }
        return result;
    }

    protected MvcResult post(String path, String token, Object payload, int expectedStatus)
            throws Exception {
        return send(MockMvcRequestBuilders.post(path), token, payload, expectedStatus);
    }

    protected MvcResult patch(String path, String token, Object payload, int expectedStatus)
            throws Exception {
        return send(MockMvcRequestBuilders.patch(path), token, payload, expectedStatus);
    }

    protected MvcResult put(String path, String token, Object payload, int expectedStatus)
            throws Exception {
        return send(MockMvcRequestBuilders.put(path), token, payload, expectedStatus);
    }

    protected MvcResult get(String path, String token, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.get(path), token, null, expectedStatus);
    }

    protected MvcResult delete(String path, String token, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.delete(path), token, null, expectedStatus);
    }

    // --- Actor fixtures ---------------------------------------------------

    protected record Actor(long id, String email, String token) {}

    protected String adminToken() throws Exception {
        if (cachedAdminToken == null) {
            MvcResult result =
                    post(
                            "/api/auth/login",
                            null,
                            Map.of("email", adminEmail, "password", adminPassword),
                            200);
            cachedAdminToken = body(result).get("token").asText();
        }
        return cachedAdminToken;
    }

    protected Actor registerClient() throws Exception {
        return register("/api/auth/register/client", "client");
    }

    /**
     * Registers a barber and immediately activates their subscription (see
     * {@link #activateSubscription(long)}) -- every existing test fixture predates the
     * subscription feature and only cares about its own concern (booking rules, ownership,
     * cancellation, ...), not this gate. Tests for the subscription feature itself should use
     * {@link #registerUnsubscribedBarber()} instead.
     */
    protected Actor registerBarber() throws Exception {
        Actor barber = register("/api/auth/register/barber", "barber");
        activateSubscription(barber.id());
        return barber;
    }

    /** Like {@link #registerBarber()} but leaves the subscription inactive, for testing the gate itself. */
    protected Actor registerUnsubscribedBarber() throws Exception {
        return register("/api/auth/register/barber", "barber");
    }

    private Actor register(String path, String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        Map<String, Object> payload =
                Map.of("name", prefix + " user", "email", email, "password", "password123");
        JsonNode registered = body(post(path, null, payload, 201));
        long id = registered.get("userId").asLong();

        User user = userRepository.findById(id).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);

        JsonNode loggedIn =
                body(
                        post(
                                "/api/auth/login",
                                null,
                                Map.of("email", email, "password", "password123"),
                                200));
        return new Actor(id, email, loggedIn.get("token").asText());
    }

    /** Directly inserts a CONFIRMED subscription payment good for 30 days, bypassing Pix/admin. */
    protected void activateSubscription(long barberId) {
        jdbc.update(
                "INSERT INTO subscription_payments (barber_id, status, amount, period_start,"
                        + " period_end, confirmed_at) VALUES (?, 'CONFIRMED', 30.00, ?, ?, now())",
                barberId,
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusDays(30));
    }

    /**
     * Registers a barber, has them request a shop, and has the admin approve it.
     * The returned actor owns a shop; read its id with {@link #shopIdOf(Actor)}.
     */
    protected Actor registerShopOwner() throws Exception {
        Actor owner = registerBarber();
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

    protected long shopIdOf(Actor barber) throws Exception {
        return body(get("/api/barbers/" + barber.id(), barber.token(), 200))
                .get("barberShopId")
                .asLong();
    }

    protected long createService(Actor owner, long shopId, int durationMinutes) throws Exception {
        Map<String, Object> payload =
                Map.of(
                        "name", "Corte " + durationMinutes,
                        "durationMinutes", durationMinutes,
                        "price", new BigDecimal("50.00"));
        return body(post("/api/barbershops/" + shopId + "/services", owner.token(), payload, 201))
                .get("id")
                .asLong();
    }

    protected void setWorkingHours(
            Actor barber, int start, int end, Integer breakStart, Integer breakEnd)
            throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "barber user");
        payload.put("delayTolerance", 0);
        payload.put("workStartHour", start);
        payload.put("workEndHour", end);
        payload.put("breakStartHour", breakStart);
        payload.put("breakEndHour", breakEnd);
        put("/api/barbers/" + barber.id(), barber.token(), payload, 200);
    }

    /** The next future Monday at the given wall-clock time. */
    protected LocalDateTime nextMondayAt(int hour, int minute) {
        LocalDateTime d =
                LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        while (d.getDayOfWeek().getValue() != 1 || !d.isAfter(LocalDateTime.now())) {
            d = d.plusDays(1);
        }
        return d;
    }

    protected Map<String, Object> bookingPayload(
            long barberId, long serviceId, LocalDateTime when) {
        return Map.of(
                "barberId", barberId,
                "serviceId", serviceId,
                "scheduledAt", when.toString(),
                "paymentMethod", "CASH");
    }

    /**
     * Moves an appointment's scheduled time into the past, bypassing the booking flow. A barber
     * can only mark an appointment COMPLETED once its scheduled time has passed, so tests that
     * only care about post-completion behavior (not that rule itself) need this to complete a
     * freshly-booked (necessarily future) appointment.
     */
    protected void backdateAppointment(long appointmentId) {
        jdbc.update(
                "UPDATE appointments SET scheduled_at = ? WHERE id = ?",
                LocalDateTime.now().minusHours(1),
                appointmentId);
    }
}
