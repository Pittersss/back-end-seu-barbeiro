package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.dto.auth.ResendCodeDTO;
import com.two_m.yourbarber.dto.auth.VerifyEmailDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.DuplicateResourceException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.UserRepository;
import com.two_m.yourbarber.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private MailService mailService;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "codeExpirationMinutes", 15L);
    }

    @Test
    void registerClient_savesClientAndReturnsToken() {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "jane@example.com", "password123", "1234");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
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
        verify(mailService).sendVerificationCode(eq("jane@example.com"), eq("Jane"), any());
    }

    @Test
    void registerClient_verifiedDuplicateEmail_throwsAndDoesNotSave() {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "jane@example.com", "password123", "1234");
        Client existing =
                Client.builder()
                        .name("Jane")
                        .email(dto.getEmail())
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .emailVerified(true)
                        .build();
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(existing));

        assertThrows(
                DuplicateResourceException.class, () -> authService.registerClient(dto));
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void registerClient_abandonedUnverifiedEmail_deletesAndReregisters() {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "jane@example.com", "password123", "1234");
        Client abandoned =
                Client.builder()
                        .name("Jane")
                        .email(dto.getEmail())
                        .password("old-encoded")
                        .role(UserRole.CLIENT)
                        .emailVerified(false)
                        .build();
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(abandoned));
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
        verify(userRepository).delete(abandoned);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerBarber_savesBarberAndReturnsToken() {
        RegisterBarberDTO dto =
                new RegisterBarberDTO(
                        "John", "john@example.com", "password123", "999", "pix-key");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
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
    void registerBarber_verifiedDuplicateEmail_throws() {
        RegisterBarberDTO dto =
                new RegisterBarberDTO(
                        "John", "john@example.com", "password123", "999", "pix-key");
        Client existing =
                Client.builder()
                        .name("John")
                        .email(dto.getEmail())
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .emailVerified(true)
                        .build();
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(existing));

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

    @Test
    void verifyEmail_correctCode_marksVerifiedAndReturnsToken() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .verificationCode("123456")
                        .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10))
                        .build();
        client.setId(1L);
        VerifyEmailDTO dto = new VerifyEmailDTO("jane@example.com", "123456");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(client));
        when(jwtTokenProvider.generateToken(client)).thenReturn("token999");

        AuthResponseDTO response = authService.verifyEmail(dto);

        assertThat(response.getToken()).isEqualTo("token999");
        assertThat(client.isEmailVerified()).isTrue();
        assertThat(client.getVerificationCode()).isNull();
    }

    @Test
    void verifyEmail_wrongCode_throws() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .verificationCode("123456")
                        .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10))
                        .build();
        VerifyEmailDTO dto = new VerifyEmailDTO("jane@example.com", "000000");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class, () -> authService.verifyEmail(dto));
    }

    @Test
    void verifyEmail_expiredCode_throws() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .verificationCode("123456")
                        .verificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1))
                        .build();
        VerifyEmailDTO dto = new VerifyEmailDTO("jane@example.com", "123456");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class, () -> authService.verifyEmail(dto));
    }

    @Test
    void verifyEmail_unknownEmail_throwsNotFound() {
        VerifyEmailDTO dto = new VerifyEmailDTO("missing@example.com", "123456");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.verifyEmail(dto));
    }

    @Test
    void resendCode_pendingUser_regeneratesAndSendsCode() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .build();
        ResendCodeDTO dto = new ResendCodeDTO("jane@example.com");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(client));

        authService.resendCode(dto);

        assertThat(client.getVerificationCode()).isNotNull();
        verify(mailService).sendVerificationCode(eq("jane@example.com"), eq("Jane"), any());
    }

    @Test
    void resendCode_alreadyVerified_throws() {
        Client client =
                Client.builder()
                        .name("Jane")
                        .email("jane@example.com")
                        .password("encoded")
                        .role(UserRole.CLIENT)
                        .emailVerified(true)
                        .build();
        ResendCodeDTO dto = new ResendCodeDTO("jane@example.com");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class, () -> authService.resendCode(dto));
        verify(userRepository, never()).save(any());
    }
}
