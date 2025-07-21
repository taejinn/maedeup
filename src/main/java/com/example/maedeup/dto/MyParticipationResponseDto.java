package com.example.maedeup.dto;

import com.example.maedeup.entity.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "내 참여 내역 응답")
public record MyParticipationResponseDto(
        @Schema(description = "참여 ID", example = "1")
        Long participationId,
        
        @Schema(description = "이벤트 ID", example = "1")
        Long eventId,
        
        @Schema(description = "이벤트 제목", example = "특별 할인 이벤트")
        String eventTitle,
        
        @Schema(description = "참여 상태", example = "SUCCESS")
        ParticipationStatus status,
        
        @Schema(description = "참여 시간", example = "2024-12-25T10:30:00")
        LocalDateTime participatedAt
) {}
