package com.example.maedeup.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "events_lottery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("LOTTERY")
@PrimaryKeyJoinColumn(name = "event_id")
public class EventLottery extends Event {

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private LocalDateTime drawTime;

    @Column(nullable = false)
    private Integer winnerCount;

    @Column(length = 36, unique = true)
    private String drawTransactionId;

    public EventLottery(User creator, String title, String description, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime drawTime, Integer winnerCount) {
        super(creator, title, description, startTime);
        this.endTime = endTime;
        this.drawTime = drawTime;
        this.winnerCount = winnerCount;
    }
    
    public void setWinnerCount(Integer winnerCount) {
        this.winnerCount = winnerCount;
    }
    
    public void setDrawTime(LocalDateTime drawTime) {
        this.drawTime = drawTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
