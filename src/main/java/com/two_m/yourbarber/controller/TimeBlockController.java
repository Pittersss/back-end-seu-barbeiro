package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.timeblock.TimeBlockPostDTO;
import com.two_m.yourbarber.dto.timeblock.TimeBlockResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.timeblock.TimeBlockService;
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
@RequestMapping("/api/barbers/{barberId}/time-blocks")
@RequiredArgsConstructor
public class TimeBlockController {

    private final TimeBlockService timeBlockService;

    @GetMapping
    public ResponseEntity<List<TimeBlockResponseDTO>> list(@PathVariable Long barberId) {
        return ResponseEntity.ok(timeBlockService.listUpcoming(barberId));
    }

    @PostMapping
    public ResponseEntity<TimeBlockResponseDTO> create(
            @PathVariable Long barberId,
            @Valid @RequestBody TimeBlockPostDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeBlockService.create(barberId, dto, currentUser.getId()));
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long barberId,
            @PathVariable Long blockId,
            @AuthenticationPrincipal User currentUser) {
        timeBlockService.delete(barberId, blockId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
