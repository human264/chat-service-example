package com.example.chatservice.mapperVo;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private String text;
    private Long memberId;
    private Long chatroomId;
    private LocalDateTime createdAt;
}