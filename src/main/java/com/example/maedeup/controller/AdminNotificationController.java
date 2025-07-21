package com.example.maedeup.controller;

import com.example.maedeup.dto.NotificationSendRequestDto;
import com.example.maedeup.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Notification", description = "관리자 알림 전송 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "모든 사용자에게 알림 전송", description = "등록된 모든 사용자에게 알림을 전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/send-all")
    public ResponseEntity<Void> sendNotificationToAllUsers(
            @Parameter(description = "알림 메시지", required = true)
            @RequestParam String message,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("모든 사용자 알림 전송 요청 - 관리자: {}, 메시지: {}", userDetails.getUsername(), message);
        notificationService.sendNotificationToAllUsers(message);
        log.info("모든 사용자 알림 전송 완료 - 관리자: {}", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이벤트 참여자에게 조건별 알림 전송", 
               description = "특정 이벤트의 참여자들에게 조건(전체/당첨/미당첨/대기)에 따라 알림을 전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/send-event")
    public ResponseEntity<Void> sendNotificationToEventParticipants(
            @Parameter(description = "알림 전송 요청 데이터", required = true)
            @Valid @RequestBody NotificationSendRequestDto requestDto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("이벤트 참여자 알림 전송 요청 - 관리자: {}, 이벤트 ID: {}, 대상: {}", 
                userDetails.getUsername(), requestDto.eventId(), requestDto.targetType());
        notificationService.sendNotificationToEventParticipants(requestDto);
        log.info("이벤트 참여자 알림 전송 완료 - 관리자: {}", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "특정 사용자에게 알림 전송", description = "지정한 사용자에게 개별 알림을 전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/send-user")
    public ResponseEntity<Void> sendNotificationToUser(
            @Parameter(description = "사용자 로그인 ID", required = true)
            @RequestParam String loginId,
            @Parameter(description = "알림 메시지", required = true)
            @RequestParam String message,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("개별 사용자 알림 전송 요청 - 관리자: {}, 대상 사용자: {}, 메시지: {}", 
                userDetails.getUsername(), loginId, message);
        notificationService.sendNotificationToUser(loginId, message);
        log.info("개별 사용자 알림 전송 완료 - 관리자: {}", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
} 