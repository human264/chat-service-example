package com.example.chatservice.dtos;

import java.time.LocalDateTime;

public record ChatMessages(
        String senderName,        // 실명 or 익명 닉네임
        String profileImageUrl,   // 프로필 사진 URL
        String message,
        LocalDateTime createdAt,  // 메시지 전송 시각
        int readCount             // 이 메시지를 읽은 사람 수
) {
}