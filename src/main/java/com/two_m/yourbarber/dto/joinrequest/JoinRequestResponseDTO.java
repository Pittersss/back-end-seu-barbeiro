package com.two_m.yourbarber.dto.joinrequest;

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
public class JoinRequestResponseDTO {

    private Long id;
    private String status;
    private String message;
    private Long barberId;
    private String barberName;
    private Long barberShopId;
    private String barberShopName;
    private LocalDateTime createdAt;
}
