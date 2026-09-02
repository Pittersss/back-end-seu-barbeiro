package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;
import com.two_m.yourbarber.dto.auth.ResendCodeDTO;
import com.two_m.yourbarber.dto.auth.VerifyEmailDTO;

public interface AuthService {

    AuthResponseDTO registerClient(RegisterClientDTO dto);

    AuthResponseDTO registerBarber(RegisterBarberDTO dto);

    AuthResponseDTO login(LoginRequestDTO dto);

    AuthResponseDTO verifyEmail(VerifyEmailDTO dto);

    void resendCode(ResendCodeDTO dto);
}
