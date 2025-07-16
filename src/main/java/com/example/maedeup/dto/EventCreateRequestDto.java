package com.example.maedeup.dto;

import com.example.maedeup.entity.EventType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventCreateRequestDto(
        @NotBlank String title,
        String description,
        @NotNull EventType eventType,
        @NotNull @Future LocalDateTime startTime,

        // 선착순(FCFS)
        Integer maxParticipants,

        // 추첨(Lottery)
        @Future LocalDateTime endTime,
        @Future LocalDateTime drawTime,
        Integer winnerCount
) {}
