package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.pix.PixPreviewDTO;
import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;
import com.two_m.yourbarber.service.pix.PixService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pix")
@RequiredArgsConstructor
public class PixController {

    private final PixService pixService;

    @PostMapping("/preview")
    public ResponseEntity<PixQrCodeResponseDTO> preview(@Valid @RequestBody PixPreviewDTO dto) {
        return ResponseEntity.ok(pixService.preview(dto));
    }
}
