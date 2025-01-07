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
     * 캐시 무효화 또는 갱신 로직을 넣을 수도 있음
     */
    @Transactional
    public Chatroom createChatroom(Member member, String title, ChatroomType type) {
        Chatroom chatroom = Chatroom.builder()
                .title(title)
                .createdAt(LocalDateTime.now())
                .hasNewMessage(false)
                .type(type)
                .build();

        chatroom = chatroomRepository.save(chatroom);

        MemberChatroomMapping creatorMapping = chatroom.addMember(member);

        if (type == ChatroomType.ANONYMOUS) {
            creatorMapping.setAliasName(generateAnonymousAlias());
        }

        memberChatroomMappingRepository.save(creatorMapping);

        // 채팅방 목록 캐시에 영향이 있으므로,
        // 필요하다면 @CacheEvict(value="chatroomList", key="#member.id") 등의 처리
        return chatroom;
    }

    /**
     * 1:1 채팅방 만들기
     */
    @Transactional
    public Chatroom createOneToOneChat(Member memberA, Member memberB) {
        Chatroom chatroom = Chatroom.builder()
                .title("1:1 채팅")
                .createdAt(LocalDateTime.now())
                .hasNewMessage(false)
                .type(ChatroomType.ONE_TO_ONE)
                .build();

        chatroom = chatroomRepository.save(chatroom);

        MemberChatroomMapping m1 = chatroom.addMember(memberA);
        MemberChatroomMapping m2 = chatroom.addMember(memberB);

        memberChatroomMappingRepository.save(m1);
        memberChatroomMappingRepository.save(m2);

        return chatroom;
    }

    /**
     * 채팅방 참여
     */
    @Transactional
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

        MemberChatroomMapping mapping = MemberChatroomMapping.builder()
                .member(member)
                .chatroom(chatroom)
                .build();

        if (chatroom.getType() == ChatroomType.ANONYMOUS) {
            mapping.setAliasName(generateAnonymousAlias());
        }

        memberChatroomMappingRepository.save(mapping);
        return true;
    }

    /**
     * 현재 채팅방의 읽음 시간 업데이트
     */
    @Transactional
    public void updateLastCheckedAt(Member member, Long currentChatroomId) {
        MemberChatroomMapping mapping = memberChatroomMappingRepository
                .findByMemberIdAndChatroomId(member.getId(), currentChatroomId)
                .orElseThrow(() -> new IllegalArgumentException("현재 채팅방 참여정보가 없습니다."));
        mapping.updateLastCheckedAt();
        memberChatroomMappingRepository.save(mapping);
    }

    /**
     * 채팅방 나가기
     * 마지막 인원 나가면 방 삭제
     */
    @Transactional
    public Boolean leaveChatroom(Member member, Long chatroomId) {
        if (!memberChatroomMappingRepository.existsByMemberIdAndChatroomId(member.getId(), chatroomId)) {
            log.info("참여하지 않는 방입니다.");
            return false;
        }

        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        chatroom.removeMember(member);

        if (chatroom.getMemberCount() == 0) {
            chatroomRepository.delete(chatroom);
        }
        return true;
    }

    /**
     * 유저가 참여 중인 모든 채팅방 (캐시 예시)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "chatroomList", key = "#member.id")
    public List<Chatroom> getChatroomList(Member member) {
        List<MemberChatroomMapping> mappingList =
                memberChatroomMappingRepository.findAllByMemberId(member.getId());

        return mappingList.stream().map(m -> {
            Chatroom chatroom = m.getChatroom();
            chatroom.setHasNewMessage(
                    messageRepository.existsByChatroomIdAndCreatedAtAfter(
                            chatroom.getId(),
                            m.getLastCheckedAt()
                    )
            );
            return chatroom;
        }).toList();
    }

    /**
     * 메시지 생성
     */
    @Transactional
    @CacheEvict(value = "messageList", key = "#chatroomId") // 메시지 변경 시 캐시 무효화
    public Message saveMessage(Member member, Long chatroomId, String text) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        Message message = Message.builder()
                .text(text)
                .member(member)
                .chatroom(chatroom)
                .createdAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    /**
     * 메시지 목록 조회 (캐시)
     * - 메시지가 많을 경우, 페이징(Offset, Limit) 또는 Cursor 방식 고려
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "messageList", key = "#chatroomId")
    public List<Message> getMessageList(Long chatroomId) {
        return messageRepository.findAllByChatroomId(chatroomId);
    }

    /**
     * 채팅방 정보 조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "chatroomInfo", key = "#chatroomId")
    public ChatroomDto getChatroom(Long chatroomId) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        return ChatroomDto.from(chatroom);
    }

    /**
     * 메시지를 읽은 사람 수 계산
     */
    @Transactional(readOnly = true)
    public int getReadCount(Message message) {
        Long chatroomId = message.getChatroom().getId();
        List<MemberChatroomMapping> mappingList =
                memberChatroomMappingRepository.findAllByChatroomId(chatroomId);

        int count = 0;
        for (MemberChatroomMapping m : mappingList) {
            if (m.getLastCheckedAt() != null &&
                    m.getLastCheckedAt().isAfter(message.getCreatedAt())) {
                count++;
            }
        }
        return count;
    }

    private String generateAnonymousAlias() {
        int randomNum = (int) (Math.random() * 9000) + 1000;
        return "익명#" + randomNum;
    }
}
