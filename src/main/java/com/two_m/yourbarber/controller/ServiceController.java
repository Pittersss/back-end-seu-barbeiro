package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.service.ServicePostPutDTO;
import com.two_m.yourbarber.dto.service.ServiceResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.ServiceService;
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
@RequestMapping("/api/barbershops/{shopId}/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    public ResponseEntity<ServiceResponseDTO> createService(
            @PathVariable Long shopId,
            @Valid @RequestBody ServicePostPutDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceService.createService(shopId, dto, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponseDTO>> listServices(@PathVariable Long shopId) {
        return ResponseEntity.ok(serviceService.listServices(shopId));
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> updateService(
            @PathVariable Long shopId,
            @PathVariable Long serviceId,
            @Valid @RequestBody ServicePostPutDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                serviceService.updateService(shopId, serviceId, dto, currentUser.getId()));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long shopId,
            @PathVariable Long serviceId,
            @AuthenticationPrincipal User currentUser) {
        serviceService.deleteService(shopId, serviceId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{serviceId}/availability")
    public ResponseEntity<ServiceResponseDTO> toggleAvailability(
            @PathVariable Long shopId,
            @PathVariable Long serviceId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                serviceService.toggleAvailability(shopId, serviceId, currentUser.getId()));
    }
}
