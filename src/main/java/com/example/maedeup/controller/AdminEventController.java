package com.example.maedeup.controller;

import com.example.maedeup.dto.EventCreateRequestDto;
import com.example.maedeup.dto.EventUpdateRequestDto;
import com.example.maedeup.dto.ParticipantResponseDto;
import com.example.maedeup.service.AdminEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    @PostMapping
    public ResponseEntity<Void> createEvent(
            @Valid @RequestBody EventCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String creatorLoginId = userDetails.getUsername();
        var createdEvent = adminEventService.createEvent(requestDto, creatorLoginId);
        return ResponseEntity.created(URI.create("/events/" + createdEvent.getId())).build();
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<Void> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequestDto requestDto
    ) {
        adminEventService.updateEvent(eventId, requestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        adminEventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/participants")
    public ResponseEntity<List<ParticipantResponseDto>> getEventParticipants(@PathVariable Long eventId) {
        List<ParticipantResponseDto> participants = adminEventService.getEventParticipants(eventId);
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/{eventId}/draw")
    public ResponseEntity<Void> drawWinners(@PathVariable Long eventId) {
        adminEventService.drawLotteryWinners(eventId);
        return ResponseEntity.ok().build();
    }
}
