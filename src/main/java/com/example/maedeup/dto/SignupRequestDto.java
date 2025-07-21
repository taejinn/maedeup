package com.example.maedeup.dto;

import com.example.maedeup.entity.RoleType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequestDto(
        @Schema(description = "로그인 아이디", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "로그인 아이디는 필수입니다")
        @Size(min = 3, max = 50, message = "로그인 아이디는 3-50자 사이여야 합니다")
        String loginId,

        @Schema(description = "닉네임", example = "테스트사용자", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 15, message = "닉네임은 2-15자 사이여야 합니다")
        String nickname,

        @Schema(description = "이메일", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "비밀번호", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 4, max = 100, message = "비밀번호는 4자 이상이어야 합니다")
        String password,

        @Schema(description = "비밀번호 확인", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호 확인은 필수입니다")
        String confirmPassword,

        @Schema(description = "사용자 역할", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "사용자 역할은 필수입니다")
        RoleType role
) {
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
} 