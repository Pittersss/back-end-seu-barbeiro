package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.auth.AuthResponseDTO;
import com.two_m.yourbarber.dto.auth.LoginRequestDTO;
import com.two_m.yourbarber.dto.auth.RegisterBarberDTO;
import com.two_m.yourbarber.dto.auth.RegisterClientDTO;

public interface AuthService {

    AuthResponseDTO registerClient(RegisterClientDTO dto);

    AuthResponseDTO registerBarber(RegisterBarberDTO dto);

    AuthResponseDTO login(LoginRequestDTO dto);
}
