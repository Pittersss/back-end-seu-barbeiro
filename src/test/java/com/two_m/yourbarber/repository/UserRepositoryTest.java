package com.two_m.yourbarber.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private UserRepository userRepository;

    @Test
    void findByEmail_existingUser_returnsUser() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .build();
        userRepository.save(client);

        Optional<User> found = userRepository.findByEmail("jane@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Jane");
    }

    @Test
    void findByEmail_nonExisting_returnsEmpty() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_existingUser_returnsTrue() {
        Client client =
                Client.builder()
                        .name("Bob")
                        .email("bob@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .build();
        userRepository.save(client);

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
    }
}
