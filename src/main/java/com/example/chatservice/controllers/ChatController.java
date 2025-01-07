package com.example.chatservice.controllers;

import com.example.chatservice.dtos.ChatMessages;
import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.entities.Chatroom;
import com.example.chatservice.entities.MemberChatroomMapping;
import com.example.chatservice.entities.Message;
import com.example.chatservice.enums.ChatroomType;
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

    // 채팅방 생성
    @PostMapping
    public ChatroomDto createChatroom(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestParam String title,
            @RequestParam(defaultValue = "MULTI") ChatroomType type
    ) {
        return ChatroomDto.from(chatService.createChatroom(user.getMember(), title, type));
    }

    // 채팅방 참여
    @PostMapping("/{chatroomId}")
    public Boolean joinChatroom(@AuthenticationPrincipal CustomOAuth2User user,
                                @PathVariable Long chatroomId,
                                @RequestParam(required = false) Long currentChatroomId) {
        return chatService.joinChatroom(user.getMember(), chatroomId, currentChatroomId);
    }

    // 채팅방 나가기
    @DeleteMapping("/{chatroomId}")
    public Boolean leaveChatroom(@AuthenticationPrincipal CustomOAuth2User user, @PathVariable Long chatroomId) {
        return chatService.leaveChatroom(user.getMember(), chatroomId);
    }

    // 채팅방 목록 조회
    @GetMapping
    public List<ChatroomDto> getChatroomList(@AuthenticationPrincipal CustomOAuth2User user) {
        List<Chatroom> chatrooms = chatService.getChatroomList(user.getMember());
        return chatrooms.stream()
                .map(ChatroomDto::from)
                .toList();
    }

    // 특정 채팅방의 메시지 목록 조회 (익명 처리 포함)
    @GetMapping("/{chatroomId}/message")
    public List<ChatMessages> getMessageList(@AuthenticationPrincipal CustomOAuth2User user,
                                             @PathVariable Long chatroomId) {
        List<Message> messageList = chatService.getMessageList(chatroomId);
        ChatroomDto chatroomDto = chatService.getChatroom(chatroomId);

        return messageList.stream().map(message -> {
            int readCount = chatService.getReadCount(message);

            String senderName = message.getMember().getNickName();
            String profileImageUrl = message.getMember().getProfileImageUrl();

            if (chatroomDto.type() == ChatroomType.ANONYMOUS) {
                // 익명 별칭
                MemberChatroomMapping mapping = message.getChatroom()
                        .getMemberChatroomMappingSet()
                        .stream()
                        .filter(m -> m.getMember().getId().equals(message.getMember().getId()))
                        .findFirst()
                        .orElse(null);

                if (mapping != null && mapping.getAliasName() != null) {
                    senderName = mapping.getAliasName();
                }
                // 익명 이미지
                profileImageUrl = "/images/anonymous.png";
            }

            return new ChatMessages(
                    senderName,
                    profileImageUrl,
                    message.getText(),
                    message.getCreatedAt(),
                    readCount
            );
        }).toList();
    }
}
