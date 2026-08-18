package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.exception.DuplicateResourceException;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.UserRepository;
import com.two_m.yourbarber.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void registerClient_savesClientAndReturnsToken() {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "jane@example.com", "password123", "1234");
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded");
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("token123");
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User u = invocation.getArgument(0);
                            u.setId(1L);
                            return u;
                        });

        AuthResponseDTO response = authService.registerClient(dto);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("CLIENT");
        assertThat(response.getName()).isEqualTo("Jane");
    }

    @Test
    void registerClient_duplicateEmail_throwsAndDoesNotSave() {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "jane@example.com", "password123", "1234");
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class, () -> authService.registerClient(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerBarber_savesBarberAndReturnsToken() {
        RegisterBarberDTO dto =
                new RegisterBarberDTO(
                        "John", "john@example.com", "password123", "999", "pix-key");
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded");
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("token456");
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User u = invocation.getArgument(0);
                            u.setId(2L);
                            return u;
                        });

        AuthResponseDTO response = authService.registerBarber(dto);

        assertThat(response.getToken()).isEqualTo("token456");
        assertThat(response.getRole()).isEqualTo("BARBER");
    }

    @Test
    void registerBarber_duplicateEmail_throws() {
        RegisterBarberDTO dto =
                new RegisterBarberDTO(
                        "John", "john@example.com", "password123", "999", "pix-key");
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class, () -> authService.registerBarber(dto));
    }

    @Test
    void login_success_returnsToken() {
        LoginRequestDTO dto = new LoginRequestDTO("jane@example.com", "password123");
        Client client =
                Client.builder()
                        .name("Jane")
                        .email(dto.getEmail())
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .build();
        client.setId(1L);
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(client));
        when(jwtTokenProvider.generateToken(client)).thenReturn("token789");

        AuthResponseDTO response = authService.login(dto);

        assertThat(response.getToken()).isEqualTo("token789");
        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_invalidCredentials_throws() {
        LoginRequestDTO dto = new LoginRequestDTO("jane@example.com", "wrongpassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(dto));
    }
}
