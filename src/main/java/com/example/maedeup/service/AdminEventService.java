package com.example.maedeup.service;

import com.example.maedeup.dto.EventCreateRequestDto;
import com.example.maedeup.dto.EventUpdateRequestDto;
import com.example.maedeup.dto.ParticipantResponseDto;
import com.example.maedeup.entity.*;
import com.example.maedeup.entity.EventType;
import com.example.maedeup.entity.ParticipationStatus;
import com.example.maedeup.repository.EventRepository;
import com.example.maedeup.repository.ParticipationRepository;
import com.example.maedeup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventService {

    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final RedisEventCounterService redisEventCounterService;

    @Transactional
    public Event createEvent(EventCreateRequestDto requestDto, String creatorLoginId) {
        User creator = userRepository.findByLoginId(creatorLoginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        Event savedEvent;
        
        if (requestDto.eventType() == EventType.FCFS) {
            EventFcfs event = new EventFcfs(
                    creator,
                    requestDto.title(),
                    requestDto.description(),
                    requestDto.startTime(),
                    requestDto.maxParticipants()
            );
            savedEvent = eventRepository.save(event);
        } else {
            EventLottery event = new EventLottery(
                    creator,
                    requestDto.title(),
                    requestDto.description(),
                    requestDto.startTime(),
                    requestDto.endTime(),
                    requestDto.drawTime(),
                    requestDto.winnerCount()
            );
            savedEvent = eventRepository.save(event);
        }

        redisEventCounterService.initializeParticipantCount(savedEvent.getId(), 0L);
        
        return savedEvent;
    }

    @Transactional
    public void updateEvent(Long eventId, EventUpdateRequestDto requestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        event.setTitle(requestDto.title());
        event.setDescription(requestDto.description());
        event.setStartTime(requestDto.startTime());
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        redisEventCounterService.clearEventData(eventId);
        eventRepository.deleteById(eventId);
    }

    public List<ParticipantResponseDto> getEventParticipants(Long eventId) {
        return participationRepository.findByEventId(eventId).stream()
                .map(p -> new ParticipantResponseDto(
                        p.getId(),
                        p.getUser().getId(),
                        p.getUser().getNickname(),
                        p.getStatus(),
                        p.getParticipatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void drawLotteryWinners(Long eventId) {
        EventLottery lotteryEvent = eventRepository.findById(eventId)
                .filter(e -> e instanceof EventLottery)
                .map(e -> (EventLottery) e)
                .orElseThrow(() -> new IllegalStateException("추첨 이벤트가 아니거나 존재하지 않습니다."));

        List<Participation> pendingParticipants = participationRepository.findByEventIdAndStatus(eventId, ParticipationStatus.PENDING);
        if (pendingParticipants.isEmpty()) {
            throw new IllegalStateException("추첨할 참여자가 없습니다.");
        }

        Collections.shuffle(pendingParticipants);

        int winnerCount = lotteryEvent.getWinnerCount();
        List<Participation> winners = pendingParticipants.subList(0, Math.min(winnerCount, pendingParticipants.size()));
        List<Participation> losers = pendingParticipants.subList(winners.size(), pendingParticipants.size());

        winners.forEach(p -> p.setStatus(ParticipationStatus.SUCCESS));
        losers.forEach(p -> p.setStatus(ParticipationStatus.FAIL));

        participationRepository.saveAll(winners);
        participationRepository.saveAll(losers);
    }
}
