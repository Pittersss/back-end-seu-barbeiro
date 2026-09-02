package com.two_m.yourbarber.dto.pix;

import java.math.BigDecimal;
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
public class PixQrCodeResponseDTO {

    private Long appointmentId;
    private String pixKey;
    private BigDecimal amount;
    private String merchantName;
    private String merchantCity;
    private String txId;
    private String pixCopyPaste;
    private String qrCodeBase64;
}
