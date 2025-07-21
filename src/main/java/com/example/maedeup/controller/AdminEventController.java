package com.example.maedeup.controller;

import com.example.maedeup.dto.EventCreateRequestDto;
import com.example.maedeup.dto.EventUpdateRequestDto;
import com.example.maedeup.dto.ParticipantResponseDto;
import com.example.maedeup.service.AdminEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Admin Event", description = "관리자 이벤트 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/events")
@Slf4j
public class AdminEventController {

    private final AdminEventService adminEventService;

    @Operation(summary = "이벤트 생성", description = "새로운 이벤트를 생성합니다. 선착순 또는 추첨 이벤트를 생성할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "이벤트 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<Void> createEvent(
            @Parameter(description = "이벤트 생성 요청 데이터", required = true)
            @Valid @RequestBody EventCreateRequestDto requestDto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String creatorLoginId = userDetails.getUsername();
        log.info("이벤트 생성 요청 - 관리자: {}, 이벤트명: {}, 타입: {}", creatorLoginId, requestDto.title(), requestDto.eventType());
        var createdEvent = adminEventService.createEvent(requestDto, creatorLoginId);
        log.info("이벤트 생성 완료 - 이벤트 ID: {}, 이벤트명: {}", createdEvent.getId(), createdEvent.getTitle());
        return ResponseEntity.created(URI.create("/events/" + createdEvent.getId())).build();
    }

    @Operation(summary = "이벤트 수정", description = "기존 이벤트의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이벤트 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{eventId}")
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId,
            @Parameter(description = "이벤트 수정 요청 데이터", required = true)
            @Valid @RequestBody EventUpdateRequestDto requestDto
    ) {
        log.info("이벤트 수정 요청 - 이벤트 ID: {}, 새 제목: {}", eventId, requestDto.title());
        adminEventService.updateEvent(eventId, requestDto);
        log.info("이벤트 수정 완료 - 이벤트 ID: {}", eventId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이벤트 삭제", description = "이벤트를 삭제합니다. 관련된 참여 기록도 함께 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "이벤트 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId) {
        log.info("이벤트 삭제 요청 - 이벤트 ID: {}", eventId);
        adminEventService.deleteEvent(eventId);
        log.info("이벤트 삭제 완료 - 이벤트 ID: {}", eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이벤트 참여자 목록 조회", description = "특정 이벤트의 참여자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여자 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{eventId}/participants")
    public ResponseEntity<List<ParticipantResponseDto>> getEventParticipants(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId) {
        log.info("이벤트 참여자 목록 조회 요청 - 이벤트 ID: {}", eventId);
        List<ParticipantResponseDto> participants = adminEventService.getEventParticipants(eventId);
        log.info("이벤트 참여자 목록 조회 완료 - 이벤트 ID: {}, 참여자 수: {}", eventId, participants.size());
        return ResponseEntity.ok(participants);
    }

    @Operation(summary = "추첨 실행", description = "추첨 이벤트의 당첨자를 추첨합니다. 추첨 이벤트에만 사용 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추첨 실행 성공"),
            @ApiResponse(responseCode = "400", description = "추첨 이벤트가 아니거나 추첨 조건에 맞지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{eventId}/draw")
    public ResponseEntity<Void> drawWinners(
            @Parameter(description = "이벤트 ID", required = true, example = "1")
            @PathVariable Long eventId) {
        log.info("추첨 실행 요청 - 이벤트 ID: {}", eventId);
        adminEventService.drawLotteryWinners(eventId);
        log.info("추첨 실행 완료 - 이벤트 ID: {}", eventId);
        return ResponseEntity.ok().build();
    }
}
