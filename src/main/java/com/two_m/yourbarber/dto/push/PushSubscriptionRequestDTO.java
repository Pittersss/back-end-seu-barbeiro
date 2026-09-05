package com.two_m.yourbarber.dto.push;

import com.two_m.yourbarber.model.enums.PushPlatform;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequestDTO {

    @NotNull private PushPlatform platform;

    private String endpoint;
    private String p256dh;
    private String authKey;
    private String expoPushToken;
}
