package com.example.maedeup.controller;

import com.example.maedeup.dto.EventDetailResponseDto;
import com.example.maedeup.dto.EventListResponseDto;
import com.example.maedeup.dto.MyParticipationResponseDto;
import com.example.maedeup.dto.NotificationResponseDto;
import com.example.maedeup.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;

@Tag(name = "Event", description = "이벤트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
@Slf4j
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 목록 조회", description = "페이지네이션된 이벤트 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이벤트 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<Page<EventListResponseDto>> getAllEvents(
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "size", description = "페이지 크기", example = "20") 
            @RequestParam(defaultValue = "20") int size,
            @Parameter(name = "sort", description = "정렬 조건 (예: id,desc)", example = "id,desc")
            @RequestParam(required = false) String sort) {
        
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
                sortObj = Sort.by(direction, sortParts[0]);
            } else {
                sortObj = Sort.by(Sort.Direction.ASC, sortParts[0]);
            }
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);
        log.info("이벤트 목록 조회 요청 - 페이지: {}, 크기: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<EventListResponseDto> events = eventService.getAllEvents(pageable);
        log.info("이벤트 목록 조회 완료 - 총 {}개 이벤트 조회", events.getTotalElements());
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "이벤트 상세 조회", description = "특정 이벤트의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이벤트 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponseDto> getEventDetails(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId) {
        log.info("이벤트 상세 조회 요청 - 이벤트 ID: {}", eventId);
        EventDetailResponseDto eventDetails = eventService.getEventDetails(eventId);
        log.info("이벤트 상세 조회 완료 - 이벤트명: {}", eventDetails.title());
        return ResponseEntity.ok(eventDetails);
    }

    @Operation(summary = "이벤트 참여", description = "특정 이벤트에 참여합니다. 인증된 사용자만 참여 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이벤트 참여 성공"),
            @ApiResponse(responseCode = "400", description = "이미 참여한 이벤트이거나 참여 조건에 맞지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{eventId}/participate")
    public ResponseEntity<Void> participateInEvent(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("이벤트 참여 요청 - 이벤트 ID: {}, 사용자: {}", eventId, userDetails.getUsername());
        eventService.participateInEvent(eventId, userDetails.getUsername());
        log.info("이벤트 참여 완료 - 이벤트 ID: {}, 사용자: {}", eventId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 참여 내역 조회", description = "로그인한 사용자의 모든 이벤트 참여 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my/participations")
    public ResponseEntity<List<MyParticipationResponseDto>> getMyParticipations(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("내 참여 내역 조회 요청 - 사용자: {}", userDetails.getUsername());
        List<MyParticipationResponseDto> participations = eventService.getMyParticipations(userDetails.getUsername());
        log.info("내 참여 내역 조회 완료 - 사용자: {}, 참여 건수: {}", userDetails.getUsername(), participations.size());
        return ResponseEntity.ok(participations);
    }

    @Operation(summary = "특정 이벤트 참여 내역 조회", description = "특정 이벤트에 대한 내 참여 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "참여 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{eventId}/my/participation")
    public ResponseEntity<MyParticipationResponseDto> getMyParticipationForEvent(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("특정 이벤트 참여 내역 조회 요청 - 이벤트 ID: {}, 사용자: {}", eventId, userDetails.getUsername());
        MyParticipationResponseDto participation = eventService.getMyParticipationForEvent(eventId, userDetails.getUsername());
        log.info("특정 이벤트 참여 내역 조회 완료 - 이벤트 ID: {}, 참여 상태: {}", eventId, participation.status());
        return ResponseEntity.ok(participation);
    }

    @Operation(summary = "참여 취소", description = "이벤트 참여를 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여 취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소할 수 없는 참여 상태"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "참여 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/participations/{participationId}")
    public ResponseEntity<Void> cancelParticipation(
            @Parameter(description = "참여 ID", required = true, example = "1")
            @PathVariable Long participationId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("참여 취소 요청 - 참여 ID: {}, 사용자: {}", participationId, userDetails.getUsername());
        eventService.cancelParticipation(participationId, userDetails.getUsername());
        log.info("참여 취소 완료 - 참여 ID: {}, 사용자: {}", participationId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 알림 조회", description = "로그인한 사용자의 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my/notifications")
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("내 알림 조회 요청 - 사용자: {}", userDetails.getUsername());
        List<NotificationResponseDto> notifications = eventService.getMyNotifications(userDetails.getUsername());
        log.info("내 알림 조회 완료 - 사용자: {}, 알림 건수: {}", userDetails.getUsername(), notifications.size());
        return ResponseEntity.ok(notifications);
    }
}
