package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.dto.auth.ResendCodeDTO;
import com.two_m.yourbarber.dto.auth.VerifyEmailDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.DuplicateResourceException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.UserRepository;
import com.two_m.yourbarber.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;

    @Value("${verification.code-expiration-minutes}")
    private long codeExpirationMinutes;

    @Override
    public AuthResponseDTO registerClient(RegisterClientDTO dto) {
        freeUpEmailIfAbandoned(dto.getEmail());

        Client client =
                Client.builder()
                        .name(dto.getName())
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .role(UserRole.CLIENT)
                        .phone(dto.getPhone())
                        .build();

        issueVerificationCode(client);
        userRepository.save(client);
        mailService.sendVerificationCode(
                client.getEmail(), client.getName(), client.getVerificationCode());
        return buildAuthResponse(client);
    }

    @Override
    public AuthResponseDTO registerBarber(RegisterBarberDTO dto) {
        freeUpEmailIfAbandoned(dto.getEmail());

        Barber barber =
                Barber.builder()
                        .name(dto.getName())
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .role(UserRole.BARBER)
                        .phone(dto.getPhone())
                        .pixKey(dto.getPixKey())
                        .available(true)
                        .build();

        issueVerificationCode(barber);
        userRepository.save(barber);
        mailService.sendVerificationCode(
                barber.getEmail(), barber.getName(), barber.getVerificationCode());
        return buildAuthResponse(barber);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        User user =
                userRepository
                        .findByEmail(dto.getEmail())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Authenticated user not found: "
                                                        + dto.getEmail()));

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponseDTO verifyEmail(VerifyEmailDTO dto) {
        User user = findByEmailOrThrow(dto.getEmail());

        if (user.isEmailVerified()) {
            throw new BusinessRuleException("Email already verified");
        }

        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(dto.getCode())
                || user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Invalid or expired verification code");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    public void resendCode(ResendCodeDTO dto) {
        User user = findByEmailOrThrow(dto.getEmail());

        if (user.isEmailVerified()) {
            throw new BusinessRuleException("Email already verified");
        }

        issueVerificationCode(user);
        userRepository.save(user);
        mailService.sendVerificationCode(
                user.getEmail(), user.getName(), user.getVerificationCode());
    }

    private User findByEmailOrThrow(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void issueVerificationCode(User user) {
        user.setVerificationCode(String.format("%06d", RANDOM.nextInt(1_000_000)));
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(codeExpirationMinutes));
    }

    /**
     * A never-verified registration left an email "taken" with no way to ever log in or
     * re-register it -- resend-code is the only way forward, and if mail was never configured
     * correctly, that path is a dead end too. Treat an unverified account as an abandoned
     * signup attempt and free up its email for a fresh one; only a verified account actually
     * blocks re-registration.
     */
    private void freeUpEmailIfAbandoned(String email) {
        userRepository
                .findByEmail(email)
                .ifPresent(
                        existing -> {
                            if (existing.isEmailVerified()) {
                                throw new DuplicateResourceException(
                                        "Email already registered: " + email);
                            }
                            userRepository.delete(existing);
                        });
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        return AuthResponseDTO.builder()
                .token(jwtTokenProvider.generateToken(user))
                .userId(user.getId())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }
}
