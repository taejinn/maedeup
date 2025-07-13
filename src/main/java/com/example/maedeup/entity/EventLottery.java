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
}
