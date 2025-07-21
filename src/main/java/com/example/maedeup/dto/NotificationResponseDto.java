package com.example.maedeup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "알림 응답")
public record NotificationResponseDto(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId,
        
        @Schema(description = "알림 메시지", example = "이벤트에 당첨되었습니다!")
        String message,
        
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        
        @Schema(description = "생성 시간", example = "2024-12-25T10:30:00")
        LocalDateTime createdAt
) {}
