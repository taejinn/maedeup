package com.example.maedeup.dto;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long notificationId,
        String message,
        boolean isRead,
        LocalDateTime createdAt
) {}
