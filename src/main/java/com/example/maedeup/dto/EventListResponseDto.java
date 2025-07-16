package com.example.maedeup.dto;

import com.example.maedeup.entity.EventType;
import java.time.LocalDateTime;

public record EventListResponseDto(
        Long eventId,
        String title,
        EventType eventType,
        LocalDateTime startTime
) {}
