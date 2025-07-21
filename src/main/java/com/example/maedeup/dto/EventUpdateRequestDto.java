package com.example.maedeup.dto;

import com.example.maedeup.entity.ResultVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Schema(description = "이벤트 수정 요청")
public record EventUpdateRequestDto(
        @Schema(description = "이벤트 제목", example = "수정된 이벤트 제목", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String title,
        
        @Schema(description = "이벤트 설명", example = "수정된 이벤트 설명입니다.")
        String description,
        
        @Schema(description = "이벤트 시작 시간", example = "2024-12-25T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @Future LocalDateTime startTime,
        
        @Schema(description = "최대 참여자 수 (선착순 이벤트만)", example = "100")
        Integer maxParticipants,
        
        @Schema(description = "종료 시간 (추첨 이벤트만)", example = "2024-12-25T17:00:00")
        LocalDateTime endTime,
        
        @Schema(description = "당첨자 수 (추첨 이벤트만)", example = "10")
        Integer winnerCount,
        
        @Schema(description = "추첨 시간 (추첨 이벤트만)", example = "2024-12-25T18:00:00")
        LocalDateTime drawTime,
        
        @Schema(description = "결과 공개 설정 (추첨 이벤트만)", example = "PUBLIC")
        ResultVisibility resultVisibility
) {}
