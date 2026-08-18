package com.two_m.yourbarber.dto.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicePostPutDTO {

    @NotBlank private String name;

    private String description;

    @NotNull
    @Positive
    private Integer durationMinutes;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    private byte[] image;
}
