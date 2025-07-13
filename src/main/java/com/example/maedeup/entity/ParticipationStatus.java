package com.example.maedeup.entity;

public enum ParticipationStatus {
    SUCCESS, // 성공(선착순) 또는 당첨(추첨)
    FAIL,    // 실패(선착순 마감)
    PENDING, // 추첨 대기중
    CANCELED // 취소됨
}
