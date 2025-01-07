package com.example.chatservice.controllers;


import com.example.chatservice.dtos.ChatMessages;
import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.enums.ChatroomType;
import com.example.chatservice.mapperVo.Member;
import com.example.chatservice.mapperVo.Message;
import com.example.chatservice.services.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/{chatroomId}")
    @SendTo("/sub/chats/{chatroomId}")
    public ChatMessages handleMessage(
            @AuthenticationPrincipal Principal principal,
            @DestinationVariable Long chatroomId,
            @Payload Map<String, String> payload
    ) {
        // principal -> CustomOAuth2User 변환 (생략)
        Member member = null; // 유저 정보 추출

        Message message = chatService.saveMessage(member, chatroomId, payload.get("message"));
        ChatroomDto chatroomDto = chatService.getChatroom(chatroomId);
        messagingTemplate.convertAndSend("/sub/chats/updates", chatroomDto);

        int readCount = chatService.getReadCount(message);

        // 익명 여부
        String senderName = member.getNickName();
        String profileImageUrl = member.getProfileImageUrl();

        if (chatroomDto.type() == ChatroomType.ANONYMOUS) {
            // mapping 검색 -> aliasName
            senderName = "익명#0000";
            profileImageUrl = "/images/anonymous.png";
        }

        return new ChatMessages(
                senderName,
                profileImageUrl,
                message.getText(),
                message.getCreatedAt(),
                readCount
        );
    }
}
