package com.example.maedeup.service;

import com.example.maedeup.dto.SignupRequestDto;
import com.example.maedeup.entity.User;
import com.example.maedeup.exception.ValidationException;
import com.example.maedeup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequestDto requestDto) {
        log.info("회원가입 처리 시작 - 로그인 ID: {}, 닉네임: {}, 역할: {}", 
                requestDto.loginId(), requestDto.nickname(), requestDto.role());
        
        // 비밀번호 확인 검증
        if (!requestDto.isPasswordMatching()) {
            throw new ValidationException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        
        // 중복 검증
        validateDuplicateUser(requestDto);
        
        // 사용자 생성
        User user = new User(
                requestDto.loginId(),
                requestDto.nickname(),
                requestDto.email(),
                passwordEncoder.encode(requestDto.password()),
                requestDto.role()
        );
        
        User savedUser = userRepository.save(user);
        log.info("회원가입 완료 - 사용자 ID: {}, 로그인 ID: {}, 역할: {}", 
                savedUser.getId(), savedUser.getLoginId(), savedUser.getRole());
        
        return savedUser;
    }
    
    private void validateDuplicateUser(SignupRequestDto requestDto) {
        // 로그인 아이디 중복 검증
        if (userRepository.findByLoginId(requestDto.loginId()).isPresent()) {
            throw new ValidationException("이미 사용 중인 로그인 아이디입니다.");
        }
        
        // 이메일 중복 검증  
        if (userRepository.findByEmail(requestDto.email()).isPresent()) {
            throw new ValidationException("이미 사용 중인 이메일입니다.");
        }
        
        // 닉네임 중복 검증
        if (userRepository.findByNickname(requestDto.nickname()).isPresent()) {
            throw new ValidationException("이미 사용 중인 닉네임입니다.");
        }
    }
    
    public boolean isLoginIdAvailable(String loginId) {
        return userRepository.findByLoginId(loginId).isEmpty();
    }
    
    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }
    
    public boolean isNicknameAvailable(String nickname) {
        return userRepository.findByNickname(nickname).isEmpty();
    }
} 