package com.two_m.yourbarber.dto.appointment;

import com.two_m.yourbarber.model.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentStatusDTO {

    @NotNull private AppointmentStatus status;
}
