package com.example.chatservice.controllers;

import com.example.chatservice.dtos.ChatMessages;
import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.entities.Chatroom;
import com.example.chatservice.entities.Message;
import com.example.chatservice.services.ChatService;
import com.example.chatservice.vos.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatroomDto createChatroom(@AuthenticationPrincipal CustomOAuth2User user, @RequestParam String title) {
        return ChatroomDto.from(chatService.createChatroom(user.getMember(), title));
    }

    @PostMapping("/{chatroomId}")
    public Boolean joinChatroom(@AuthenticationPrincipal CustomOAuth2User user,
                                @PathVariable Long chatroomId, @RequestParam(required = false) Long currentChatroomId) {
        return chatService.joinChatroom(user.getMember(), chatroomId, currentChatroomId);
    }

    @DeleteMapping("/{chatroomId}")
    public Boolean leaveChatroom(@AuthenticationPrincipal CustomOAuth2User user, @PathVariable Long chatroomId) {
        return chatService.leaveChatroom(user.getMember(), chatroomId);
    }

    @GetMapping
    public List<ChatroomDto> getChatroomList(@AuthenticationPrincipal CustomOAuth2User user) {
        List<Chatroom> chatrooms = chatService.getChatroomList(user.getMember());
        return chatrooms.stream().map(ChatroomDto::from).toList();
    }


    @GetMapping("/{chatroomId}/message")
    public List<ChatMessages> getMessageList(@PathVariable Long chatroomId) {
        List<Message> messageList = chatService.getMessageList(chatroomId);
        return messageList.stream()
                .map(message -> new ChatMessages(message.getMember().getNickName(), message.getText()))
                .toList();
    }
}
