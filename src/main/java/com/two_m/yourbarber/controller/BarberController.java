package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.barber.BarberPostPutDTO;
import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.barber.BarberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
public class BarberController {

    private final BarberService barberService;

    @GetMapping("/{id}")
    public ResponseEntity<BarberResponseDTO> getBarber(@PathVariable Long id) {
        return ResponseEntity.ok(barberService.getBarber(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarberResponseDTO> updateBarber(
            @PathVariable Long id,
            @Valid @RequestBody BarberPostPutDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(barberService.updateBarber(id, dto, currentUser.getId()));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<BarberResponseDTO> toggleAvailability(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(barberService.toggleAvailability(id, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBarber(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        barberService.deleteBarber(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
