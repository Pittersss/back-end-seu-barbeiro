package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.dto.auth.ResendCodeDTO;
import com.two_m.yourbarber.dto.auth.VerifyEmailDTO;
import com.two_m.yourbarber.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/client")
    public ResponseEntity<AuthResponseDTO> registerClient(
            @Valid @RequestBody RegisterClientDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerClient(dto));
    }

    @PostMapping("/register/barber")
    public ResponseEntity<AuthResponseDTO> registerBarber(
            @Valid @RequestBody RegisterBarberDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerBarber(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponseDTO> verifyEmail(@Valid @RequestBody VerifyEmailDTO dto) {
        return ResponseEntity.ok(authService.verifyEmail(dto));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody ResendCodeDTO dto) {
        authService.resendCode(dto);
        return ResponseEntity.ok().build();
    }
}
