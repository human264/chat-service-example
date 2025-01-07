package com.example.chatservice.mapperVo;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberChatroomMapping {
    private Long id;
    private Long memberId;
    private Long chatroomId;

    private LocalDateTime lastCheckedAt;
    private String aliasName;  // 익명 방일 때 사용

    // 편의 메서드
    public void updateLastCheckedAt() {
        this.lastCheckedAt = LocalDateTime.now();
    }
}