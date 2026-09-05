package com.two_m.yourbarber.dto.push;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushUnsubscribeRequestDTO {

    private String endpoint;
    private String expoPushToken;
}
