package com.example.chatservice.dtos;

import com.example.chatservice.entities.Chatroom;
import com.example.chatservice.enums.ChatroomType;

import java.time.LocalDateTime;
public record ChatroomDto(
        Long id,
        String title,
        Boolean hasNewMessage,
        Integer memberCount,
        LocalDateTime createAt,
        ChatroomType type  // 추가: 어떤 타입의 방인지
) {
    public static ChatroomDto from(Chatroom chatroom) {
        return new ChatroomDto(
                chatroom.getId(),
                chatroom.getTitle(),
                chatroom.getHasNewMessage(),
                chatroom.getMemberCount(),
                chatroom.getCreatedAt(),
                chatroom.getType()  // 추가
        );
    }
}