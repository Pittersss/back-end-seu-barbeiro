package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.clientblock.ClientBlockPostDTO;
import com.two_m.yourbarber.dto.clientblock.ClientBlockResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.clientblock.ClientBlockService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barbers/{barberId}/blocked-clients")
@RequiredArgsConstructor
public class ClientBlockController {

    private final ClientBlockService clientBlockService;

    @GetMapping
    public ResponseEntity<List<ClientBlockResponseDTO>> list(
            @PathVariable Long barberId, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(clientBlockService.list(barberId, currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<ClientBlockResponseDTO> block(
            @PathVariable Long barberId,
            @Valid @RequestBody ClientBlockPostDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientBlockService.block(barberId, dto, currentUser.getId()));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> unblock(
            @PathVariable Long barberId,
            @PathVariable Long clientId,
            @AuthenticationPrincipal User currentUser) {
        clientBlockService.unblock(barberId, clientId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
