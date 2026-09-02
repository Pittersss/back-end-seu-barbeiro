package com.two_m.yourbarber.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDTO {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 30)
    private String phone;

    /** Base64 image payload; null/blank clears the picture. Guard-railed to keep rows small. */
    @Size(max = 700_000)
    private String avatarBase64;
}
