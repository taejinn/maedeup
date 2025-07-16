package com.example.maedeup.dto;

import com.example.maedeup.entity.ParticipationStatus;
import java.time.LocalDateTime;

public record ParticipantResponseDto(
        Long participationId,
        Long userId,
        String username,
        ParticipationStatus status,
        LocalDateTime participatedAt
) {}
