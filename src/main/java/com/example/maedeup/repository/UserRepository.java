package com.example.maedeup.repository;

import com.example.maedeup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByNickname(String nickname);
}
