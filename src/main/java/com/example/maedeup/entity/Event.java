package com.example.maedeup.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "event_type")
public abstract class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultVisibility resultVisibility;

    @Lob
    private String announcementMessage;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime canceledAt;

    protected Event(User creator, String title, String description, LocalDateTime startTime) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.resultVisibility = ResultVisibility.PRIVATE;
    }
}
