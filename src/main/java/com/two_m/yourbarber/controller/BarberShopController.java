package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopPostPutDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.BarberShopService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barbershops")
@RequiredArgsConstructor
public class BarberShopController {

    private final BarberShopService barberShopService;

    @PostMapping
    public ResponseEntity<BarberShopRequestResponseDTO> requestCreation(
            @Valid @RequestBody BarberShopRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(barberShopService.requestCreation(dto, currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberShopResponseDTO> getBarberShop(@PathVariable Long id) {
        return ResponseEntity.ok(barberShopService.getBarberShop(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarberShopResponseDTO> updateBarberShop(
            @PathVariable Long id,
            @Valid @RequestBody BarberShopPostPutDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                barberShopService.updateBarberShop(id, dto, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBarberShop(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        barberShopService.deleteBarberShop(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/accepting-barbers")
    public ResponseEntity<BarberShopResponseDTO> toggleAcceptingBarbers(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                barberShopService.toggleAcceptingBarbers(id, currentUser.getId()));
    }

    @GetMapping("/{id}/barbers")
    public ResponseEntity<List<BarberResponseDTO>> listBarbers(@PathVariable Long id) {
        return ResponseEntity.ok(barberShopService.listBarbers(id));
    }

    @PatchMapping("/{shopId}/barbers/{barberId}/availability")
    public ResponseEntity<BarberResponseDTO> toggleBarberAvailability(
            @PathVariable Long shopId,
            @PathVariable Long barberId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                barberShopService.toggleBarberAvailability(
                        shopId, barberId, currentUser.getId()));
    }

    @DeleteMapping("/{shopId}/barbers/{barberId}")
    public ResponseEntity<Void> removeBarberFromShop(
            @PathVariable Long shopId,
            @PathVariable Long barberId,
            @AuthenticationPrincipal User currentUser) {
        barberShopService.removeBarberFromShop(shopId, barberId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
