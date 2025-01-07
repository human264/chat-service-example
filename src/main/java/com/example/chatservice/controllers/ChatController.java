package com.example.chatservice.controllers;

import com.example.chatservice.dtos.ChatMessages;
import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.enums.ChatroomType;
import com.example.chatservice.mapperVo.Chatroom;
import com.example.chatservice.mapperVo.MemberChatroomMapping;
import com.example.chatservice.mapperVo.Message;
import com.example.chatservice.services.ChatService;
import com.example.chatservice.vos.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatroomDto createChatroom(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestParam String title,
            @RequestParam(defaultValue = "MULTI") ChatroomType type
    ) {
        Chatroom c = chatService.createChatroom(user.getMember(), title, type);
        return ChatroomDto.from(c);
    }

    @PostMapping("/{chatroomId}")
    public boolean joinChatroom(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long chatroomId,
            @RequestParam(required = false) Long currentChatroomId
    ) {
        return chatService.joinChatroom(user.getMember(), chatroomId, currentChatroomId);
    }

    @DeleteMapping("/{chatroomId}")
    public boolean leaveChatroom(@AuthenticationPrincipal CustomOAuth2User user,
                                 @PathVariable Long chatroomId) {
        return chatService.leaveChatroom(user.getMember(), chatroomId);
    }

    @GetMapping
    public List<ChatroomDto> getChatroomList(@AuthenticationPrincipal CustomOAuth2User user) {
        List<Chatroom> rooms = chatService.getChatroomList(user.getMember());
        return rooms.stream().map(ChatroomDto::from).toList();
    }

    @GetMapping("/{chatroomId}/message")
    public List<ChatMessages> getMessageList(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long chatroomId
    ) {
        List<Message> messages = chatService.getMessageList(chatroomId);
        ChatroomDto chatroom = chatService.getChatroom(chatroomId);

        return messages.stream().map(msg -> {
            int readCount = chatService.getReadCount(msg);

            // 익명 처리
            String senderName = user.getMember().getNickName();
            String profileUrl = user.getMember().getProfileImageUrl();

            if (chatroom.type() == ChatroomType.ANONYMOUS) {
                MemberChatroomMapping mapping =
                        // 매퍼에서 직접 조회해도 됨
                        // 여기서는 message의 memberId 기준으로, chatroomId 기준으로 매핑 검색
                        null; // 생략(로직 구현)
                // mapping.getAliasName();
                senderName = "[익명닉네임]";
                profileUrl = "/images/anonymous.png";
            }

            return new ChatMessages(
                    senderName,
                    profileUrl,
                    msg.getText(),
                    msg.getCreatedAt(),
                    readCount
            );
        }).toList();
    }
}