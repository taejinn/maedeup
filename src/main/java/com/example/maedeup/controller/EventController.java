package com.example.maedeup.controller;

import com.example.maedeup.dto.EventDetailResponseDto;
import com.example.maedeup.dto.EventListResponseDto;
import com.example.maedeup.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventListResponseDto>> getAllEvents(Pageable pageable) {
        Page<EventListResponseDto> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponseDto> getEventDetails(@PathVariable Long eventId) {
        EventDetailResponseDto eventDetails = eventService.getEventDetails(eventId);
        return ResponseEntity.ok(eventDetails);
    }

    @PostMapping("/{eventId}/participate")
    public ResponseEntity<Void> participateInEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        eventService.participateInEvent(eventId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
