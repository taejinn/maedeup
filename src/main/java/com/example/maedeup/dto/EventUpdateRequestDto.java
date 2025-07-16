package com.example.maedeup.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record EventUpdateRequestDto(
        @NotBlank String title,
        String description,
        @Future LocalDateTime startTime
) {}
