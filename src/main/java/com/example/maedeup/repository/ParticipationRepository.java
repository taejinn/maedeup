package com.example.maedeup.repository;

import com.example.maedeup.entity.Participation;
import com.example.maedeup.entity.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByEventId(Long eventId);
    List<Participation> findByEventIdAndStatus(Long eventId, ParticipationStatus status);
    List<Participation> findByUserId(Long userId);
    Optional<Participation> findByUserIdAndEventId(Long userId, Long eventId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    long countByEventIdAndStatus(Long eventId, ParticipationStatus status);
    long countByEventId(Long eventId);
}
