package com.two_m.yourbarber.dto.barber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String avatarBase64;
    private String pixKey;
    private boolean available;
    private int delayTolerance;
    private int workStartHour;
    private int workEndHour;
    private Integer breakStartHour;
    private Integer breakEndHour;
    private Long barberShopId;
}
