package com.two_m.yourbarber.dto.timeblock;

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
public class TimeBlockResponseDTO {

    private Long id;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String reason;
    private Long barberId;
}
