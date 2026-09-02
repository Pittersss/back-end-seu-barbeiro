package com.two_m.yourbarber.dto.clientblock;

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
public class ClientBlockResponseDTO {

    private Long id;
    private Long clientId;
    private String clientName;
    private String clientPhone;
    private String reason;
    private LocalDateTime createdAt;
}
