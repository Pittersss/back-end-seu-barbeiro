package com.two_m.yourbarber.dto.subscription;

import com.two_m.yourbarber.model.enums.SubscriptionStatus;
import java.time.LocalDate;
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
public class SubscriptionStatusDTO {

    private SubscriptionStatus status;
    private LocalDate periodEnd;
}
