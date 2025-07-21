package com.example.maedeup.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "events_fcfs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("FCFS")
@PrimaryKeyJoinColumn(name = "event_id")
public class EventFcfs extends Event {
    private Integer maxParticipants;

    public EventFcfs(User creator, String title, String description, LocalDateTime startTime, Integer maxParticipants) {
        super(creator, title, description, startTime);
        this.maxParticipants = maxParticipants;
    }
    
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
}
