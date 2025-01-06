package com.example.chatservice.dtos;

import com.example.chatservice.entities.Chatroom;

import java.time.LocalDateTime;

public record ChatroomDto(
        Long id,
        String title,
        Boolean hasNewMessage,
        Integer memberCount,
        LocalDateTime createAt
) {
    public static ChatroomDto from(Chatroom chatroom) {
        return new ChatroomDto(
                chatroom.getId(),
                chatroom.getTitle(),
                chatroom.getHasNewMessage(),
                chatroom.getMemberChatroomMappingSet().size(),
                chatroom.getCreatedAt());
    }
}
