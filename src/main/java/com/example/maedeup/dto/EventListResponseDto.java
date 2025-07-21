package com.example.maedeup.dto;

import com.example.maedeup.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "이벤트 목록 응답")
public record EventListResponseDto(
        @Schema(description = "이벤트 ID", example = "1")
        Long eventId,
        
        @Schema(description = "이벤트 제목", example = "특별 할인 이벤트")
        String title,
        
        @Schema(description = "이벤트 타입", example = "FCFS")
        EventType eventType,
        
        @Schema(description = "이벤트 시작 시간", example = "2024-12-25T10:00:00")
        LocalDateTime startTime
) {}
