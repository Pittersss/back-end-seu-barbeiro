package com.two_m.yourbarber.dto.barber;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BarberPostPutDTO {

    @NotBlank private String name;

    private String phone;

    private String pixKey;

    @Min(0)
    private int delayTolerance;

    @Min(0)
    @Max(24)
    private int workStartHour;

    @Min(0)
    @Max(24)
    private int workEndHour;

    @Min(0)
    @Max(24)
    private Integer breakStartHour;

    @Min(0)
    @Max(24)
    private Integer breakEndHour;
}
