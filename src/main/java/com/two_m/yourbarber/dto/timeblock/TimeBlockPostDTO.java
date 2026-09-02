package com.two_m.yourbarber.dto.timeblock;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlockPostDTO {

    @NotNull private LocalDateTime startsAt;

    @NotNull private LocalDateTime endsAt;

    private String reason;
}
