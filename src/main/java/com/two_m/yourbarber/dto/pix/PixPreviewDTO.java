package com.two_m.yourbarber.dto.pix;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PixPreviewDTO {

    @NotBlank
    @Size(max = 77)
    private String pixKey;

    @NotBlank
    @Size(max = 25)
    private String merchantName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @Size(max = 25)
    private String txId;
}
