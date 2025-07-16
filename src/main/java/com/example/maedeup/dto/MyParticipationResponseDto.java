package com.example.maedeup.dto;

import com.example.maedeup.entity.ParticipationStatus;
import java.time.LocalDateTime;

public record MyParticipationResponseDto(
        Long participationId,
        Long eventId,
        String eventTitle,
        ParticipationStatus status,
        LocalDateTime participatedAt
) {}
