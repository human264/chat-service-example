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

    /**
     * 채팅방 생성
     */
    @PostMapping
    public ChatroomDto createChatroom(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestParam String title,
            @RequestParam(defaultValue = "MULTI") ChatroomType type // 디폴트로 일반 다대다
    ) {
        return ChatroomDto.from(chatService.createChatroom(user.getMember(), title, type));
    }

    /**
     * 채팅방 참여
     */
    @PostMapping("/{chatroomId}")
    public Boolean joinChatroom(@AuthenticationPrincipal CustomOAuth2User user,
                                @PathVariable Long chatroomId,
                                @RequestParam(required = false) Long currentChatroomId) {
        return chatService.joinChatroom(user.getMember(), chatroomId, currentChatroomId);
    }

    /**
     * 채팅방 나가기
     */
    @DeleteMapping("/{chatroomId}")
    public Boolean leaveChatroom(@AuthenticationPrincipal CustomOAuth2User user, @PathVariable Long chatroomId) {
        return chatService.leaveChatroom(user.getMember(), chatroomId);
    }

    /**
     * 채팅방 목록 조회
     */
    @GetMapping
    public List<ChatroomDto> getChatroomList(@AuthenticationPrincipal CustomOAuth2User user) {
        List<Chatroom> chatrooms = chatService.getChatroomList(user.getMember());
        return chatrooms.stream().map(ChatroomDto::from).toList();
    }

    /**
     * 해당 채팅방의 메시지 목록 조회
     */
    @GetMapping("/{chatroomId}/message")
    public List<ChatMessages> getMessageList(@PathVariable Long chatroomId) {
        List<Message> messageList = chatService.getMessageList(chatroomId);

        // 채팅방 타입 확인 (익명 방이면 aliasName 사용)
        ChatroomDto chatroomDto = chatService.getChatroom(chatroomId);
        ChatroomType type = chatroomDto.type();

        return messageList.stream().map(message -> {
            // 메시지를 읽은 사람 수
            int readCount = chatService.getReadCount(message);

            // 익명 방일 경우, 해당 user의 aliasName 조회
            MemberChatroomMapping mapping = null;
            if (type == ChatroomType.ANONYMOUS) {
                mapping = // 특정 member-chatroom 매핑 조회
                        // (DB에서 findByMemberIdAndChatroomId 하거나,
                        //  message.getChatroom().getMemberChatroomMappingSet()를 순회하여 찾음)
                        message.getChatroom()
                                .getMemberChatroomMappingSet()
                                .stream()
                                .filter(m -> m.getMember().getId().equals(message.getMember().getId()))
                                .findFirst().orElse(null);
            }

            // 익명 방이면 aliasName, 아니면 실제 nickName
            String senderName = (type == ChatroomType.ANONYMOUS && mapping != null)
                    ? mapping.getAliasName()
                    : message.getMember().getNickName();

            // 익명 방이면 프로필 이미지를 숨긴다면 null,
            // 또는 공통 이미지를 표시한다면 그 URL,
            // 아니면 그대로 실제 프로필 URL을 노출
            String profileImageUrl = (type == ChatroomType.ANONYMOUS)
                    ? "/images/anonymous.png"   // 예시: 공통 익명 이미지
                    : message.getMember().getProfileImageUrl();

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