package com.example.maedeup.service;

import com.example.maedeup.dto.EventCreateRequestDto;
import com.example.maedeup.dto.EventUpdateRequestDto;
import com.example.maedeup.dto.ParticipantResponseDto;
import com.example.maedeup.entity.*;
import com.example.maedeup.entity.EventType;
import com.example.maedeup.entity.EventFcfs;
import com.example.maedeup.entity.EventLottery;
import com.example.maedeup.entity.ParticipationStatus;
import com.example.maedeup.exception.EntityNotFoundException;
import com.example.maedeup.exception.ValidationException;
import com.example.maedeup.repository.EventRepository;
import com.example.maedeup.repository.ParticipationRepository;
import com.example.maedeup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminEventService {

    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final RedisEventCounterService redisEventCounterService;
    private final RedisLockService redisLockService;

    @Transactional
    public Event createEvent(EventCreateRequestDto requestDto, String creatorLoginId) {
        log.info("이벤트 생성 처리 시작 - 관리자: {}, 이벤트명: {}, 타입: {}", 
                creatorLoginId, requestDto.title(), requestDto.eventType());
        
        // 요청 데이터 검증
        validateEventCreateRequest(requestDto);
        
        User creator = userRepository.findByLoginId(creatorLoginId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 관리자입니다."));

        Event savedEvent;
        
        if (requestDto.eventType() == EventType.FCFS) {
            log.info("선착순 이벤트 생성 - 최대 참여자: {}", requestDto.maxParticipants());
            EventFcfs event = new EventFcfs(
                    creator,
                    requestDto.title(),
                    requestDto.description(),
                    requestDto.startTime(),
                    requestDto.maxParticipants()
            );
            savedEvent = eventRepository.save(event);
        } else {
            log.info("추첨 이벤트 생성 - 당첨자 수: {}, 추첨 시간: {}", requestDto.winnerCount(), requestDto.drawTime());
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
        log.info("이벤트 생성 완료 - 이벤트 ID: {}, 이벤트명: {}", savedEvent.getId(), savedEvent.getTitle());
        
        return savedEvent;
    }

    private void validateEventCreateRequest(EventCreateRequestDto requestDto) {
        if (requestDto.eventType() == null) {
            throw new ValidationException("이벤트 타입은 필수입니다.");
        }
        
        if (requestDto.title() == null || requestDto.title().trim().isEmpty()) {
            throw new ValidationException("이벤트 제목은 필수입니다.");
        }
        
        if (requestDto.startTime() == null) {
            throw new ValidationException("이벤트 시작 시간은 필수입니다.");
        }
        
        if (requestDto.eventType() == EventType.FCFS) {
            if (requestDto.maxParticipants() == null || requestDto.maxParticipants() <= 0) {
                throw new ValidationException("선착순 이벤트는 최대 참여자 수(양수)가 필요합니다.");
            }
        } else if (requestDto.eventType() == EventType.LOTTERY) {
            if (requestDto.endTime() == null) {
                throw new ValidationException("추첨 이벤트는 종료 시간이 필요합니다.");
            }
            if (requestDto.drawTime() == null) {
                throw new ValidationException("추첨 이벤트는 추첨 시간이 필요합니다.");
            }
            if (requestDto.winnerCount() == null || requestDto.winnerCount() <= 0) {
                throw new ValidationException("추첨 이벤트는 당첨자 수(양수)가 필요합니다.");
            }
            if (requestDto.endTime().isBefore(requestDto.startTime())) {
                throw new ValidationException("종료 시간은 시작 시간보다 이후여야 합니다.");
            }
            if (requestDto.drawTime().isBefore(requestDto.endTime())) {
                throw new ValidationException("추첨 시간은 종료 시간보다 이후여야 합니다.");
            }
        }
    }

    @Transactional
    public void updateEvent(Long eventId, EventUpdateRequestDto requestDto) {
        log.info("이벤트 수정 처리 시작 - 이벤트 ID: {}, 새 제목: {}", eventId, requestDto.title());
        
        String lockKey = "event:update:" + eventId;
        redisLockService.executeWithLock(lockKey, () -> {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

            String oldTitle = event.getTitle();
            
            event.setTitle(requestDto.title());
            event.setDescription(requestDto.description());
            event.setStartTime(requestDto.startTime());
            
            if (event instanceof EventFcfs fcfsEvent) {
                log.info("선착순 이벤트 수정 - 이벤트 ID: {}, 기존 최대 참여자: {}, 새 최대 참여자: {}", 
                        eventId, fcfsEvent.getMaxParticipants(), requestDto.maxParticipants());
                fcfsEvent.setMaxParticipants(requestDto.maxParticipants());
            } else if (event instanceof EventLottery lotteryEvent) {
                log.info("추첨 이벤트 수정 - 이벤트 ID: {}, 기존 당첨자 수: {}, 새 당첨자 수: {}", 
                        eventId, lotteryEvent.getWinnerCount(), requestDto.winnerCount());
                
                if (requestDto.endTime() != null) {
                    lotteryEvent.setEndTime(requestDto.endTime());
                }
                if (requestDto.winnerCount() != null) {
                    lotteryEvent.setWinnerCount(requestDto.winnerCount());
                }
                if (requestDto.drawTime() != null) {
                    lotteryEvent.setDrawTime(requestDto.drawTime());
                }
                if (requestDto.resultVisibility() != null) {
                    event.setResultVisibility(requestDto.resultVisibility());
                }
            }
            
            log.info("이벤트 수정 완료 - 이벤트 ID: {}, 기존 제목: {}, 새 제목: {}", eventId, oldTitle, requestDto.title());
        });
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        log.info("이벤트 삭제 처리 시작 - 이벤트 ID: {}", eventId);
        
        String lockKey = "event:delete:" + eventId;
        redisLockService.executeWithLock(lockKey, () -> {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
                    
                    List<Participation> participations = participationRepository.findByEventId(eventId);
        if (!participations.isEmpty()) {
            throw new ValidationException("참여자가 있는 이벤트는 삭제할 수 없습니다.");
        }
            
            redisEventCounterService.clearEventData(eventId);
            log.info("Redis 이벤트 데이터 삭제 완료 - 이벤트 ID: {}", eventId);
            eventRepository.deleteById(eventId);
            log.info("이벤트 삭제 완료 - 이벤트 ID: {}", eventId);
        });
    }

    public List<ParticipantResponseDto> getEventParticipants(Long eventId) {
        log.info("이벤트 참여자 목록 조회 시작 - 이벤트 ID: {}", eventId);
        List<ParticipantResponseDto> participants = participationRepository.findByEventId(eventId).stream()
                .map(p -> new ParticipantResponseDto(
                        p.getId(),
                        p.getUser().getId(),
                        p.getUser().getNickname(),
                        p.getUser().getLoginId(),
                        p.getStatus(),
                        p.getParticipatedAt()
                ))
                .collect(Collectors.toList());
        log.info("이벤트 참여자 목록 조회 완료 - 이벤트 ID: {}, 참여자 수: {}", eventId, participants.size());
        return participants;
    }

    @Transactional
    public void drawLotteryWinners(Long eventId) {
        log.info("추첨 실행 시작 - 이벤트 ID: {}", eventId);
        
        String lockKey = "lottery:draw:" + eventId;
        redisLockService.executeWithLock(lockKey, () -> {
            processLotteryDraw(eventId);
        });
        
        log.info("추첨 실행 완료 - 이벤트 ID: {}", eventId);
    }
    
    private void processLotteryDraw(Long eventId) {
        EventLottery lotteryEvent = eventRepository.findById(eventId)
                .filter(e -> e instanceof EventLottery)
                .map(e -> (EventLottery) e)
                .orElseThrow(() -> new ValidationException("추첨 이벤트가 아니거나 존재하지 않습니다."));

        List<Participation> pendingParticipants = participationRepository.findByEventIdAndStatus(eventId, ParticipationStatus.PENDING);
        if (pendingParticipants.isEmpty()) {
            throw new ValidationException("추첨할 참여자가 없습니다.");
        }

        List<Participation> alreadyDrawnParticipants = participationRepository.findByEventIdAndStatus(eventId, ParticipationStatus.SUCCESS);
        if (!alreadyDrawnParticipants.isEmpty()) {
            throw new ValidationException("이미 추첨이 완료된 이벤트입니다.");
        }

        log.info("추첨 대상자 확인 완료 - 이벤트 ID: {}, 총 참여자: {}, 당첨자 수: {}", 
                eventId, pendingParticipants.size(), lotteryEvent.getWinnerCount());

        Collections.shuffle(pendingParticipants);

        int winnerCount = lotteryEvent.getWinnerCount();
        List<Participation> winners = pendingParticipants.subList(0, Math.min(winnerCount, pendingParticipants.size()));
        List<Participation> losers = pendingParticipants.subList(winners.size(), pendingParticipants.size());

        log.info("추첨 결과 결정 - 이벤트 ID: {}, 당첨자: {}명, 낙첨자: {}명", 
                eventId, winners.size(), losers.size());

        winners.forEach(p -> p.setStatus(ParticipationStatus.SUCCESS));
        losers.forEach(p -> p.setStatus(ParticipationStatus.FAIL));

        participationRepository.saveAll(winners);
        participationRepository.saveAll(losers);
    }
}
