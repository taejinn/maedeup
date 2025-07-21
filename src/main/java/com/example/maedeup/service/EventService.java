package com.example.maedeup.service;

import com.example.maedeup.dto.EventDetailResponseDto;
import com.example.maedeup.dto.EventListResponseDto;
import com.example.maedeup.dto.MyParticipationResponseDto;
import com.example.maedeup.dto.NotificationResponseDto;
import com.example.maedeup.entity.*;
import com.example.maedeup.entity.ParticipationStatus;
import com.example.maedeup.exception.EntityNotFoundException;
import com.example.maedeup.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RedisLockService redisLockService;
    private final RedisEventCounterService redisEventCounterService;

    public Page<EventListResponseDto> getAllEvents(Pageable pageable) {
        log.info("이벤트 목록 조회 시작 - 페이지: {}, 크기: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<EventListResponseDto> events = eventRepository.findAll(pageable)
                .map(event -> new EventListResponseDto(
                        event.getId(),
                        event.getTitle(),
                        (event instanceof EventFcfs) ? EventType.FCFS : EventType.LOTTERY,
                        event.getStartTime()
                ));
        log.info("이벤트 목록 조회 완료 - 조회된 이벤트 수: {}", events.getNumberOfElements());
        return events;
    }

    public EventDetailResponseDto getEventDetails(Long eventId) {
        log.info("이벤트 상세 조회 시작 - 이벤트 ID: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이벤트입니다."));
        log.info("이벤트 상세 조회 완료 - 이벤트명: {}, 타입: {}", event.getTitle(), 
                event instanceof EventFcfs ? "선착순" : "추첨");
        return EventDetailResponseDto.fromEntity(event);
    }

    @Transactional
    public void participateInEvent(Long eventId, String loginId) {
        log.info("이벤트 참여 처리 시작 - 이벤트 ID: {}, 사용자: {}", eventId, loginId);
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String lockKey = redisLockService.getEventLockKey(eventId);
        log.info("분산 락 획득 시도 - 이벤트 ID: {}", eventId);
        redisLockService.executeWithLock(lockKey, () -> {
            processEventParticipation(eventId, user);
        });
        log.info("이벤트 참여 처리 완료 - 이벤트 ID: {}, 사용자: {}", eventId, loginId);
    }

    private void processEventParticipation(Long eventId, User user) {
        log.info("이벤트 참여 처리 내부 로직 시작 - 이벤트 ID: {}, 사용자 ID: {}", eventId, user.getId());
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        validateEventParticipation(event, user);
        log.info("이벤트 참여 검증 완료 - 이벤트 ID: {}, 사용자 ID: {}", eventId, user.getId());

        ParticipationStatus status;

        if (event instanceof EventFcfs fcfsEvent) {
            log.info("선착순 이벤트 참여 처리 - 이벤트 ID: {}, 최대 참여자: {}", eventId, fcfsEvent.getMaxParticipants());
            status = handleFcfsParticipation(eventId, user, fcfsEvent);
        } else if (event instanceof EventLottery) {
            log.info("추첨 이벤트 참여 처리 - 이벤트 ID: {}", eventId);
            status = handleLotteryParticipation(eventId, user);
        } else {
            throw new IllegalStateException("알 수 없는 이벤트 타입입니다.");
        }

        log.info("참여 상태 결정됨 - 이벤트 ID: {}, 사용자 ID: {}, 상태: {}", eventId, user.getId(), status);

        Participation participation = Participation.builder()
                .user(user)
                .event(event)
                .status(status)
                .participatedAt(LocalDateTime.now())
                .build();

        participationRepository.save(participation);
        log.info("참여 기록 저장 완료 - 이벤트 ID: {}, 사용자 ID: {}", eventId, user.getId());
    }

    private void validateEventParticipation(Event event, User user) {
        if (Boolean.TRUE.equals(redisEventCounterService.isUserParticipated(event.getId(), user.getId()))) {
            throw new IllegalStateException("이미 참여한 이벤트입니다.");
        }

        if (participationRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new IllegalStateException("이미 참여한 이벤트입니다.");
        }

        if (LocalDateTime.now().isBefore(event.getStartTime())) {
            throw new IllegalStateException("이벤트가 아직 시작되지 않았습니다.");
        }

        if (event.getCanceledAt() != null) {
            throw new IllegalStateException("취소된 이벤트입니다.");
        }
    }

    private ParticipationStatus handleFcfsParticipation(Long eventId, User user, EventFcfs fcfsEvent) {
        if (Boolean.TRUE.equals(redisEventCounterService.canParticipate(eventId, fcfsEvent.getMaxParticipants(), user.getId()))) {
            redisEventCounterService.incrementParticipantCount(eventId, user.getId());
            return ParticipationStatus.SUCCESS;
        } else {
            return ParticipationStatus.FAIL;
        }
    }

    private ParticipationStatus handleLotteryParticipation(Long eventId, User user) {
        redisEventCounterService.incrementParticipantCount(eventId, user.getId());
        return ParticipationStatus.PENDING;
    }

    @Transactional
    public void cancelParticipation(Long participationId, String loginId) {
        log.info("참여 취소 처리 시작 - 참여 ID: {}, 사용자: {}", participationId, loginId);
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참여 기록입니다."));

        if (!participation.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 참여 기록만 취소할 수 있습니다.");
        }

        Event event = participation.getEvent();
        log.info("참여 취소 검증 완료 - 참여 ID: {}, 이벤트 ID: {}, 이벤트명: {}", 
                participationId, event.getId(), event.getTitle());
        
        String lockKey = redisLockService.getEventLockKey(event.getId());
        log.info("참여 취소를 위한 분산 락 획득 시도 - 이벤트 ID: {}", event.getId());
        redisLockService.executeWithLock(lockKey, () -> {
            processCancelParticipation(participation, event);
        });
        log.info("참여 취소 처리 완료 - 참여 ID: {}, 사용자: {}", participationId, loginId);
    }

    private void processCancelParticipation(Participation participation, Event event) {
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

        redisEventCounterService.decrementParticipantCount(event.getId(), participation.getUser().getId());
        
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
