package com.example.chatservice.services;


import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.entities.Chatroom;
import com.example.chatservice.entities.Member;
import com.example.chatservice.entities.MemberChatroomMapping;
import com.example.chatservice.entities.Message;
import com.example.chatservice.enums.ChatroomType;
import com.example.chatservice.repository.ChatroomRepository;
import com.example.chatservice.repository.MemberChatroomMappingRepository;
import com.example.chatservice.repository.MemberRepository;
import com.example.chatservice.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final ChatroomRepository chatroomRepository;
    private final MemberChatroomMappingRepository memberChatroomMappingRepository;

    /**
     * 채팅방 생성
     * @param member   채팅방 생성자
     * @param title    방 제목
     * @param type     방 타입 (ONE_TO_ONE, MULTI, ANONYMOUS)
     */
    public Chatroom createChatroom(Member member, String title, ChatroomType type) {
        Chatroom chatroom = Chatroom.builder()
                .title(title)
                .createdAt(LocalDateTime.now())
                .hasNewMessage(false)
                .type(type)
                .build();

        chatroom = chatroomRepository.save(chatroom);

        // 방 생성 시, 만든 사람은 자동 참여
        MemberChatroomMapping memberChatroomMapping = chatroom.addMember(member);

        // 익명 방이면 익명 닉네임 생성 (ex. "익명#랜덤ID")
        if (type == ChatroomType.ANONYMOUS) {
            memberChatroomMapping.setAliasName(generateAnonymousAlias());
        }

        memberChatroomMapping = memberChatroomMappingRepository.save(memberChatroomMapping);

        return chatroom;
    }

    /**
     * 1:1 채팅방 만들기 (필요 시 사용)
     */
    public Chatroom createOneToOneChat(Member memberA, Member memberB) {
        // title은 예시로 "1:1 대화방" 정도로 지정
        Chatroom chatroom = Chatroom.builder()
                .title("1:1 대화방")
                .createdAt(LocalDateTime.now())
                .hasNewMessage(false)
                .type(ChatroomType.ONE_TO_ONE)
                .build();

        chatroom = chatroomRepository.save(chatroom);

        // 서로 참여
        MemberChatroomMapping m1 = chatroom.addMember(memberA);
        MemberChatroomMapping m2 = chatroom.addMember(memberB);

        memberChatroomMappingRepository.save(m1);
        memberChatroomMappingRepository.save(m2);

        return chatroom;
    }

    /**
     * 채팅방 참여
     */
    public Boolean joinChatroom(Member member, Long newChatroomId, Long currentChatroomId) {
        if (currentChatroomId != null) {
            updateLastCheckedAt(member, currentChatroomId);
        }

        if (memberChatroomMappingRepository.existsByMemberIdAndChatroomId(member.getId(), newChatroomId)) {
            log.info("이미 참여한 채팅방입니다.");
            return false;
        }

        Chatroom chatroom = chatroomRepository.findById(newChatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        MemberChatroomMapping memberChatroomMapping = MemberChatroomMapping.builder()
                .member(member)
                .chatroom(chatroom)
                .build();

        // 익명 방일 경우, aliasName 생성
        if (chatroom.getType() == ChatroomType.ANONYMOUS) {
            memberChatroomMapping.setAliasName(generateAnonymousAlias());
        }

        memberChatroomMappingRepository.save(memberChatroomMapping);
        return true;
    }

    private void updateLastCheckedAt(Member member, Long currentChatroomId) {
        MemberChatroomMapping memberChatroomMapping =
                memberChatroomMappingRepository.findByMemberIdAndChatroomId(member.getId(), currentChatroomId)
                        .orElseThrow(() -> new IllegalArgumentException("현재 채팅방 참여정보가 없습니다."));
        memberChatroomMapping.updateLastCheckedAt();
        memberChatroomMappingRepository.save(memberChatroomMapping);
    }

    /**
     * 채팅방 나가기
     */
    @Transactional
    public Boolean leaveChatroom(Member member, Long chatroomId) {
        if (!memberChatroomMappingRepository.existsByMemberIdAndChatroomId(member.getId(), chatroomId)) {
            log.info("참여하지 않는 방입니다.");
            return false;
        }

        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        // 매핑 제거 (orphanRemoval = true라면, set에서 제거하면 DB에서도 제거됨)
        chatroom.removeMember(member);

        // 남은 인원이 0명이면 방 삭제(선택 사항)
        if (chatroom.getMemberCount() == 0) {
            chatroomRepository.delete(chatroom);
        }

        return true;
    }

    /**
     * 사용자가 참여한 전체 채팅방 목록 가져오기
     */
    public List<Chatroom> getChatroomList(Member member) {
        List<MemberChatroomMapping> memberChatroomMappingList =
                memberChatroomMappingRepository.findAllByMemberId(member.getId());

        return memberChatroomMappingList.stream()
                .map(memberChatroomMapping -> {
                    Chatroom chatroom = memberChatroomMapping.getChatroom();
                    chatroom.setHasNewMessage(
                            messageRepository.existsByChatroomIdAndCreatedAtAfter(
                                    chatroom.getId(),
                                    memberChatroomMapping.getLastCheckedAt()
                            )
                    );
                    return chatroom;
                })
                .toList();
    }

    /**
     * 메시지 저장
     */
    public Message saveMessage(Member member, Long chatroomId, String text) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        Message message = Message.builder()
                .text(text)
                .member(member)
                .chatroom(chatroom)
                .createdAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);
        return message;
    }

    /**
     * 특정 채팅방의 모든 메시지 조회
     */
    public List<Message> getMessageList(Long chatroomId) {
        return messageRepository.findAllByChatroomId(chatroomId);
    }

    @Transactional(readOnly = true)
    public ChatroomDto getChatroom(Long chatroomId) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        return ChatroomDto.from(chatroom);
    }

    /**
     * 메시지를 읽은 사람 수 계산
     */
    public int getReadCount(Message message) {
        Long chatroomId = message.getChatroom().getId();
        List<MemberChatroomMapping> mappingList =
                memberChatroomMappingRepository.findAllByChatroomId(chatroomId);

        // lastCheckedAt > message.createdAt 이면 읽은 것으로 판단
        int count = 0;
        for (MemberChatroomMapping mapping : mappingList) {
            if (mapping.getLastCheckedAt() != null &&
                    mapping.getLastCheckedAt().isAfter(message.getCreatedAt())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 익명 닉네임 생성 예시
     */
    private String generateAnonymousAlias() {
        // 예시: "익명#1234" 형태
        int randomNum = (int) (Math.random() * 9000) + 1000;
        return "익명#" + randomNum;
    }
}
