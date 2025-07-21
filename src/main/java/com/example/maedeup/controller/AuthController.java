package com.example.maedeup.controller;

import com.example.maedeup.dto.SignupRequestDto;
import com.example.maedeup.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "중복된 정보 (아이디, 이메일, 닉네임)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        log.info("회원가입 API 요청 - 로그인 ID: {}, 역할: {}", requestDto.loginId(), requestDto.role());
        userService.signup(requestDto);
        log.info("회원가입 API 완료 - 로그인 ID: {}", requestDto.loginId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "로그인 아이디 중복 확인", description = "로그인 아이디의 사용 가능 여부를 확인합니다.")
    @GetMapping("/check/loginId")
    public ResponseEntity<Boolean> checkLoginIdAvailability(
            @Parameter(description = "확인할 로그인 아이디") @RequestParam String loginId) {
        boolean available = userService.isLoginIdAvailable(loginId);
        return ResponseEntity.ok(available);
    }

    @Operation(summary = "이메일 중복 확인", description = "이메일의 사용 가능 여부를 확인합니다.")
    @GetMapping("/check/email")
    public ResponseEntity<Boolean> checkEmailAvailability(
            @Parameter(description = "확인할 이메일") @RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(available);
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 사용 가능 여부를 확인합니다.")
    @GetMapping("/check/nickname")
    public ResponseEntity<Boolean> checkNicknameAvailability(
            @Parameter(description = "확인할 닉네임") @RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(available);
    }
} 