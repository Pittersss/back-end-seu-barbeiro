package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.joinrequest.JoinRequestDecisionDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestPostDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.JoinRequestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barbershops/{shopId}/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    @PostMapping
    public ResponseEntity<JoinRequestResponseDTO> requestToJoin(
            @PathVariable Long shopId,
            @RequestBody JoinRequestPostDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(joinRequestService.requestToJoin(shopId, dto, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<JoinRequestResponseDTO>> listRequests(
            @PathVariable Long shopId, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(joinRequestService.listRequests(shopId, currentUser.getId()));
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<JoinRequestResponseDTO> decideRequest(
            @PathVariable Long shopId,
            @PathVariable Long requestId,
            @RequestBody JoinRequestDecisionDTO decision,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                joinRequestService.decideRequest(
                        shopId, requestId, decision, currentUser.getId()));
    }
}
