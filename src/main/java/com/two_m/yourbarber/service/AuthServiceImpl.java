package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.exception.DuplicateResourceException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.UserRepository;
import com.two_m.yourbarber.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponseDTO registerClient(RegisterClientDTO dto) {
        assertEmailAvailable(dto.getEmail());

        Client client =
                Client.builder()
                        .name(dto.getName())
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .role(UserRole.CLIENT)
                        .phone(dto.getPhone())
                        .build();

        userRepository.save(client);
        return buildAuthResponse(client);
    }

    @Override
    public AuthResponseDTO registerBarber(RegisterBarberDTO dto) {
        assertEmailAvailable(dto.getEmail());

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

        userRepository.save(barber);
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

    private void assertEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
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
