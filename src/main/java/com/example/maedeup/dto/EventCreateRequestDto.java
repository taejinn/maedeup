package com.example.maedeup.dto;

import com.example.maedeup.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "이벤트 생성 요청")
public record EventCreateRequestDto(
        @Schema(description = "이벤트 제목", example = "특별 할인 이벤트", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String title,
        
        @Schema(description = "이벤트 설명", example = "선착순 100명에게 50% 할인 쿠폰을 드립니다.")
        String description,
        
        @Schema(description = "이벤트 타입", example = "FCFS", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull EventType eventType,
        
        @Schema(description = "이벤트 시작 시간", example = "2024-12-25T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Future LocalDateTime startTime,

        // 선착순(FCFS)
        @Schema(description = "최대 참여자 수 (선착순 이벤트용)", example = "100")
        Integer maxParticipants,

        // 추첨(Lottery)
        @Schema(description = "이벤트 종료 시간 (추첨 이벤트용)", example = "2024-12-25T23:59:59")
        @Future LocalDateTime endTime,
        
        @Schema(description = "추첨 시간 (추첨 이벤트용)", example = "2024-12-26T10:00:00")
        @Future LocalDateTime drawTime,
        
        @Schema(description = "당첨자 수 (추첨 이벤트용)", example = "10")
        Integer winnerCount
) {}
