package com.two_m.yourbarber.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.dto.auth.ResendCodeDTO;
import com.two_m.yourbarber.dto.auth.VerifyEmailDTO;
import com.two_m.yourbarber.security.CustomUserDetailsService;
import com.two_m.yourbarber.security.JwtTokenProvider;
import com.two_m.yourbarber.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class
        })
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerClient_validRequest_returns201WithToken() throws Exception {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "jane@example.com", "password123", "1234");
        when(authService.registerClient(any()))
                .thenReturn(
                        AuthResponseDTO.builder()
                                .token("token123")
                                .userId(1L)
                                .role("CLIENT")
                                .name("Jane")
                                .build());

        mockMvc.perform(
                        post("/api/auth/register/client")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void registerClient_invalidEmail_returns400() throws Exception {
        RegisterClientDTO dto =
                new RegisterClientDTO("Jane", "not-an-email", "password123", "1234");

        mockMvc.perform(
                        post("/api/auth/register/client")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validRequest_returns200WithToken() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("jane@example.com", "password123");
        when(authService.login(any()))
                .thenReturn(
                        AuthResponseDTO.builder()
                                .token("token456")
                                .userId(1L)
                                .role("CLIENT")
                                .name("Jane")
                                .build());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token456"));
    }

    @Test
    void verifyEmail_validRequest_returns200WithToken() throws Exception {
        VerifyEmailDTO dto = new VerifyEmailDTO("jane@example.com", "123456");
        when(authService.verifyEmail(any()))
                .thenReturn(
                        AuthResponseDTO.builder()
                                .token("token789")
                                .userId(1L)
                                .role("CLIENT")
                                .name("Jane")
                                .build());

        mockMvc.perform(
                        post("/api/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token789"));
    }

    @Test
    void resendCode_validRequest_returns200() throws Exception {
        ResendCodeDTO dto = new ResendCodeDTO("jane@example.com");

        mockMvc.perform(
                        post("/api/auth/resend-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
