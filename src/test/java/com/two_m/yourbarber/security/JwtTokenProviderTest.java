package com.two_m.yourbarber.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider =
                new JwtTokenProvider(
                        "test-secret-key-that-is-long-enough-for-hs256-signing-1234567890",
                        3_600_000L);
    }

    private Client client() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .build();
        client.setId(1L);
        return client;
    }

    @Test
    void generateToken_thenValidate_succeeds() {
        String token = jwtTokenProvider.generateToken(client());

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void generateToken_thenExtractEmail_matchesUser() {
        String token = jwtTokenProvider.generateToken(client());

        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("jane@example.com");
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-real-token")).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        JwtTokenProvider shortLived =
                new JwtTokenProvider(
                        "test-secret-key-that-is-long-enough-for-hs256-signing-1234567890",
                        1L);
        String token = shortLived.generateToken(client());

        Thread.sleep(10);

        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
