package com.two_m.yourbarber.dto.push;

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
public class VapidPublicKeyResponseDTO {

    private String publicKey;
}
