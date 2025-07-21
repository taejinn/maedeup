package com.example.maedeup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "알림 전송 요청")
public record NotificationSendRequestDto(
        @Schema(description = "이벤트 ID (특정 이벤트 참여자에게 전송 시)", example = "1")
        Long eventId,
        
        @Schema(description = "알림 메시지", example = "축하합니다! 이벤트에 당첨되셨습니다.", required = true)
        @NotBlank(message = "알림 메시지는 필수입니다.")
        String message,
        
        @Schema(description = "전송 대상 타입", example = "ALL", 
                allowableValues = {"ALL", "SUCCESS", "FAIL", "PENDING"})
        String targetType
) {} 