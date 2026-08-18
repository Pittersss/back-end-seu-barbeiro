package com.two_m.yourbarber.dto.barbershop;

import java.time.LocalDateTime;
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
public class BarberShopRequestResponseDTO {

    private Long id;
    private String status;
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private Long requesterId;
    private String requesterName;
    private LocalDateTime createdAt;
}
