package com.example.maedeup.dto;

import com.example.maedeup.entity.Event;
import com.example.maedeup.entity.EventFcfs;
import com.example.maedeup.entity.EventLottery;
import com.example.maedeup.entity.EventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventDetailResponseDto(
        Long eventId,
        String title,
        String description,
        EventType eventType,
        LocalDateTime startTime,

        // fcfs
        Integer maxParticipants,

        // lottery
        LocalDateTime endTime,
        LocalDateTime drawTime,
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
