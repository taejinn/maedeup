package com.example.maedeup.service;

import com.example.maedeup.dto.EventDetailResponseDto;
import com.example.maedeup.dto.EventListResponseDto;
import com.example.maedeup.dto.MyParticipationResponseDto;
import com.example.maedeup.dto.NotificationResponseDto;
import com.example.maedeup.entity.*;
import com.example.maedeup.entity.ParticipationStatus;
import com.example.maedeup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Page<EventListResponseDto> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(event -> new EventListResponseDto(
                        event.getId(),
                        event.getTitle(),
                        (event instanceof EventFcfs) ? EventType.FCFS : EventType.LOTTERY,
                        event.getStartTime()
                ));
    }

    public EventDetailResponseDto getEventDetails(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
        return EventDetailResponseDto.fromEntity(event);
    }

    @Transactional
    public void participateInEvent(Long eventId, String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Event event = eventRepository.findByIdWithPessimisticLock(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        if (participationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new IllegalStateException("이미 참여한 이벤트입니다.");
        }

        if (LocalDateTime.now().isBefore(event.getStartTime())) {
            throw new IllegalStateException("이벤트가 아직 시작되지 않았습니다.");
        }

        ParticipationStatus status;

        if (event instanceof EventFcfs fcfsEvent) {
            long successCount = participationRepository.countByEventIdAndStatus(eventId, ParticipationStatus.SUCCESS);
            if (successCount < fcfsEvent.getMaxParticipants()) {
                status = ParticipationStatus.SUCCESS;
            } else {
                status = ParticipationStatus.FAIL;
            }
        } else if (event instanceof EventLottery) {
            status = ParticipationStatus.PENDING;
        } else {
            throw new IllegalStateException("알 수 없는 이벤트 타입입니다.");
        }

        Participation participation = Participation.builder()
                .user(user)
                .event(event)
                .status(status)
                .participatedAt(LocalDateTime.now())
                .build();

        participationRepository.save(participation);
    }

    @Transactional
    public void cancelParticipation(Long participationId, String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참여 기록입니다."));

        if (!participation.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 참여 기록만 취소할 수 있습니다.");
        }

        Event event = participation.getEvent();
        LocalDateTime now = LocalDateTime.now();

        if (event instanceof EventLottery lotteryEvent) {
            if (now.isAfter(lotteryEvent.getDrawTime())) {
                throw new IllegalStateException("이미 추첨이 진행된 이벤트는 참여를 취소할 수 없습니다.");
            }
        } else {
            if (now.isAfter(event.getStartTime())) {
                throw new IllegalStateException("이미 시작된 이벤트는 참여를 취소할 수 없습니다.");
            }
        }
        participationRepository.delete(participation);
    }

    public List<MyParticipationResponseDto> getMyParticipations(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return participationRepository.findByUserId(user.getId()).stream()
                .map(p -> new MyParticipationResponseDto(
                        p.getId(),
                        p.getEvent().getId(),
                        p.getEvent().getTitle(),
                        p.getStatus(),
                        p.getParticipatedAt()
                ))
                .collect(Collectors.toList());
    }

    public MyParticipationResponseDto getMyParticipationForEvent(Long eventId, String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Participation participation = participationRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이벤트에 대한 참여 기록이 없습니다."));

        return new MyParticipationResponseDto(
                participation.getId(),
                participation.getEvent().getId(),
                participation.getEvent().getTitle(),
                participation.getStatus(),
                participation.getParticipatedAt()
        );
    }

    public List<NotificationResponseDto> getMyNotifications(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(n -> new NotificationResponseDto(
                        n.getId(),
                        n.getMessage(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
