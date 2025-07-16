package com.example.maedeup.repository;

import com.example.maedeup.entity.Participation;
import com.example.maedeup.entity.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByEventId(Long eventId);
    List<Participation> findByEventIdAndStatus(Long eventId, ParticipationStatus status);
}
