package com.example.maedeup.dto;

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
        @Future LocalDateTime startTime
) {}
