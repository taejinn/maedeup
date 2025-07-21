package com.example.maedeup.service;

import com.example.maedeup.dto.NotificationSendRequestDto;
import com.example.maedeup.entity.*;
import com.example.maedeup.exception.EntityNotFoundException;
import com.example.maedeup.repository.EventRepository;
import com.example.maedeup.repository.NotificationRepository;
import com.example.maedeup.repository.ParticipationRepository;
import com.example.maedeup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    /**
     * 모든 사용자에게 알림 전송
     */
    public void sendNotificationToAllUsers(String message) {
        log.info("모든 사용자에게 알림 전송 시작 - 메시지: {}", message);
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            Notification notification = new Notification(user, message);
            notificationRepository.save(notification);
        }
        
        log.info("모든 사용자에게 알림 전송 완료 - 전송 대상: {}명", users.size());
    }

    /**
     * 특정 이벤트 참여자에게 조건별 알림 전송
     */
    public void sendNotificationToEventParticipants(NotificationSendRequestDto requestDto) {
        log.info("이벤트 참여자 알림 전송 시작 - 이벤트 ID: {}, 대상: {}, 메시지: {}", 
                requestDto.eventId(), requestDto.targetType(), requestDto.message());

        Event event = eventRepository.findById(requestDto.eventId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이벤트입니다."));

        List<Participation> participations = getTargetParticipations(event.getId(), requestDto.targetType());
        
        int sentCount = 0;
        for (Participation participation : participations) {
            Notification notification = new Notification(
                    participation.getUser(), 
                    event, 
                    requestDto.message()
            );
            notificationRepository.save(notification);
            sentCount++;
        }

        log.info("이벤트 참여자 알림 전송 완료 - 전송 대상: {}명", sentCount);
    }

    /**
     * 전송 대상 타입에 따른 참여자 목록 조회
     */
    private List<Participation> getTargetParticipations(Long eventId, String targetType) {
        return switch (targetType) {
            case "ALL" -> participationRepository.findByEventId(eventId);
            case "SUCCESS" -> participationRepository.findByEventIdAndStatus(eventId, ParticipationStatus.SUCCESS);
            case "FAIL" -> participationRepository.findByEventIdAndStatus(eventId, ParticipationStatus.FAIL);
            case "PENDING" -> participationRepository.findByEventIdAndStatus(eventId, ParticipationStatus.PENDING);
            default -> throw new IllegalArgumentException("잘못된 전송 대상 타입입니다: " + targetType);
        };
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotificationToUser(String loginId, String message) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));
        
        Notification notification = new Notification(user, message);
        notificationRepository.save(notification);
        
        log.info("사용자 알림 전송 완료 - 사용자: {}, 메시지: {}", loginId, message);
    }
} 