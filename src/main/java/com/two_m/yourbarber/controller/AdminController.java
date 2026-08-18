package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.barbershop.BarberShopRequestDecisionDTO;
import com.two_m.yourbarber.dto.barbershop.BarberShopRequestResponseDTO;
import com.two_m.yourbarber.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/barbershop-requests")
    public ResponseEntity<List<BarberShopRequestResponseDTO>> listPendingRequests() {
        return ResponseEntity.ok(adminService.listPendingRequests());
    }

    @PatchMapping("/barbershop-requests/{id}")
    public ResponseEntity<BarberShopRequestResponseDTO> decideRequest(
            @PathVariable Long id, @RequestBody BarberShopRequestDecisionDTO decision) {
        return ResponseEntity.ok(adminService.decideRequest(id, decision.isApproved()));
    }
}
