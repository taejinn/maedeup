package com.example.maedeup.dto;

import com.example.maedeup.entity.Event;
import com.example.maedeup.entity.EventFcfs;
import com.example.maedeup.entity.EventLottery;
import com.example.maedeup.entity.EventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "이벤트 상세 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventDetailResponseDto(
        @Schema(description = "이벤트 ID", example = "1")
        Long eventId,
        
        @Schema(description = "이벤트 제목", example = "특별 할인 이벤트")
        String title,
        
        @Schema(description = "이벤트 설명", example = "선착순 100명에게 50% 할인 쿠폰을 드립니다.")
        String description,
        
        @Schema(description = "이벤트 타입", example = "FCFS")
        EventType eventType,
        
        @Schema(description = "이벤트 시작 시간", example = "2024-12-25T10:00:00")
        LocalDateTime startTime,

        // fcfs
        @Schema(description = "최대 참여자 수 (선착순 이벤트)", example = "100")
        Integer maxParticipants,

        // lottery
        @Schema(description = "이벤트 종료 시간 (추첨 이벤트)", example = "2024-12-25T23:59:59")
        LocalDateTime endTime,
        
        @Schema(description = "추첨 시간 (추첨 이벤트)", example = "2024-12-26T10:00:00")
        LocalDateTime drawTime,
        
        @Schema(description = "당첨자 수 (추첨 이벤트)", example = "10")
        Integer winnerCount
) {
    public static EventDetailResponseDto fromEntity(Event event) {
        if (event instanceof EventFcfs fcfs) {
            return new EventDetailResponseDto(
                    fcfs.getId(), fcfs.getTitle(), fcfs.getDescription(), EventType.FCFS, fcfs.getStartTime(),
                    fcfs.getMaxParticipants(), null, null, null
            );
        } else if (event instanceof EventLottery lottery) {
            return new EventDetailResponseDto(
                    lottery.getId(), lottery.getTitle(), lottery.getDescription(), EventType.LOTTERY, lottery.getStartTime(),
                    null, lottery.getEndTime(), lottery.getDrawTime(), lottery.getWinnerCount()
            );
        }
        // 이벤트 type이 null 일 때 (혹시 모르니)
        return new EventDetailResponseDto(event.getId(), event.getTitle(), event.getDescription(), null, event.getStartTime(), null, null, null, null);
    }
}
