package com.example.maedeup.dto;

import com.example.maedeup.entity.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "참여자 정보 응답")
public record ParticipantResponseDto(
        @Schema(description = "참여 ID", example = "1")
        Long participationId,
        
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        
        @Schema(description = "사용자 이름", example = "홍길동")
        String username,
        
        @Schema(description = "참여 상태", example = "SUCCESS")
        ParticipationStatus status,
        
        @Schema(description = "참여 시간", example = "2024-12-25T10:30:00")
        LocalDateTime participatedAt
) {}
