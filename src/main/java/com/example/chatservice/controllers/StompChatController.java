package com.example.chatservice.controllers;


import com.example.chatservice.dtos.ChatMessages;
import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.entities.Message;
import com.example.chatservice.services.ChatService;
import com.example.chatservice.vos.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    public ChatMessages handleMessage(@AuthenticationPrincipal Principal principal,
                                      @DestinationVariable Long chatroomId,
                                      @Payload Map<String, String> payload) {

        log.info("{} sent {}", principal.getName(), payload, chatroomId);

        CustomOAuth2User user = (CustomOAuth2User) ((AbstractAuthenticationToken) principal).getPrincipal();
        Message message = chatService.saveMessage(user.getMember(), chatroomId, payload.get("message"));
        ChatroomDto chatroomDto = chatService.getChatroom(chatroomId);

        messagingTemplate.convertAndSend("/sub/chats/updates", chatroomDto);

        return new ChatMessages(principal.getName(), payload.get("message"));
    }
}