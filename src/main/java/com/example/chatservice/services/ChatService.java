package com.example.chatservice.services;


import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.enums.ChatroomType;
import com.example.chatservice.mapper.ChatroomMapper;

import com.example.chatservice.mapper.MemberChatroomMappingMapper;
import com.example.chatservice.mapper.MessageMapper;
import com.example.chatservice.mapperVo.Chatroom;
import com.example.chatservice.mapperVo.Member;
import com.example.chatservice.mapperVo.MemberChatroomMapping;
import com.example.chatservice.mapperVo.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private final ChatroomMapper chatroomMapper;
    private final MemberChatroomMappingMapper memberChatroomMappingMapper;
    private final MessageMapper messageMapper;

    @Transactional
    public Chatroom createChatroom(Member creator, String title, ChatroomType type) {
        Chatroom chatroom = Chatroom.builder()
                .title(title)
                .createdAt(LocalDateTime.now())
                .hasNewMessage(false)
                .type(type)
                .build();

        chatroomMapper.insertChatroom(chatroom);
        // chatroom.getId()가 생성됨 (LAST_INSERT_ID)

        MemberChatroomMapping mapping = MemberChatroomMapping.builder()
                .chatroomId(chatroom.getId())
                .memberId(creator.getId())
                .build();

        if (type == ChatroomType.ANONYMOUS) {
            mapping.setAliasName(generateAnonymousAlias());
        }
        memberChatroomMappingMapper.insertMapping(mapping);

        return chatroom;
    }

    @Transactional
    public boolean joinChatroom(Member member, Long newChatroomId, Long currentChatroomId) {
        if (currentChatroomId != null) {
            updateLastCheckedAt(member, currentChatroomId);
        }

        boolean alreadyExists = memberChatroomMappingMapper
                .existsByMemberIdAndChatroomId(member.getId(), newChatroomId);
        if (alreadyExists) {
            log.info("이미 참여한 채팅방");
            return false;
        }
        // 방 정보 조회
        Chatroom chatroom = chatroomMapper.findById(newChatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        MemberChatroomMapping mapping = MemberChatroomMapping.builder()
                .memberId(member.getId())
                .chatroomId(chatroom.getId())
                .build();

        if (chatroom.getType() == ChatroomType.ANONYMOUS) {
            mapping.setAliasName(generateAnonymousAlias());
        }
        memberChatroomMappingMapper.insertMapping(mapping);

        return true;
    }

    @Transactional
    public void updateLastCheckedAt(Member member, Long chatroomId) {
        MemberChatroomMapping mapping = memberChatroomMappingMapper
                .findByMemberIdAndChatroomId(member.getId(), chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("매핑 정보가 없음"));
        mapping.updateLastCheckedAt();
        memberChatroomMappingMapper.updateMapping(mapping);
    }

    @Transactional
    public boolean leaveChatroom(Member member, Long chatroomId) {
        boolean exists = memberChatroomMappingMapper
                .existsByMemberIdAndChatroomId(member.getId(), chatroomId);
        if (!exists) {
            log.info("참여하지 않은 방");
            return false;
        }
        // 방 정보
        Chatroom chatroom = chatroomMapper.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        // 매핑 삭제
        memberChatroomMappingMapper.deleteByMemberIdAndChatroomId(member.getId(), chatroomId);

        // 남은 사람이 0명이면 방 삭제
        List<MemberChatroomMapping> remainList = memberChatroomMappingMapper.findAllByChatroomId(chatroomId);
        if (remainList.isEmpty()) {
            chatroomMapper.deleteById(chatroomId);
        }

        return true;
    }

    /**
     * 유저가 참여 중인 채팅방 리스트 (캐시)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "chatroomList", key = "#member.id")
    public List<Chatroom> getChatroomList(Member member) {
        // 모든 매핑 중 memberId가 일치하는 것 조회
        List<MemberChatroomMapping> mappingList = memberChatroomMappingMapper.findAllByMemberId(member.getId());

        // 각 mapping마다 Chatroom을 조회
        List<Chatroom> results = new ArrayList<>();
        for (MemberChatroomMapping m : mappingList) {
            Chatroom c = chatroomMapper.findById(m.getChatroomId()).orElse(null);
            if (c == null) continue;

            // hasNewMessage 체크
            Boolean existsNew = messageMapper.existsNewMessage(c.getId(), m.getLastCheckedAt());
            c.setHasNewMessage(existsNew);

            results.add(c);
        }
        return results;
    }

    /**
     * 메시지 저장
     */
    @Transactional
    @CacheEvict(value = "messageList", key = "#chatroomId")
    public Message saveMessage(Member member, Long chatroomId, String text) {
        Message msg = Message.builder()
                .chatroomId(chatroomId)
                .memberId(member.getId())
                .text(text)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insertMessage(msg);
        return msg;
    }

    /**
     * 메시지 목록 조회 (캐시 적용)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "messageList", key = "#chatroomId")
    public List<Message> getMessageList(Long chatroomId) {
        return messageMapper.findAllByChatroomId(chatroomId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "chatroomInfo", key = "#chatroomId")
    public ChatroomDto getChatroom(Long chatroomId) {
        Chatroom c = chatroomMapper.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("방 없음"));
        return ChatroomDto.from(c);
    }

    @Transactional(readOnly = true)
    public int getReadCount(Message message) {
        Long chatroomId = message.getChatroomId();
        List<MemberChatroomMapping> list = memberChatroomMappingMapper.findAllByChatroomId(chatroomId);

        int cnt = 0;
        for (MemberChatroomMapping m : list) {
            if (m.getLastCheckedAt() != null && m.getLastCheckedAt().isAfter(message.getCreatedAt())) {
                cnt++;
            }
        }
        return cnt;
    }

    private String generateAnonymousAlias() {
        int rand = (int)(Math.random() * 9000) + 1000;
        return "익명#" + rand;
    }
}
