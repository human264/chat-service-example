package com.example.chatservice.services;


import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.entities.Chatroom;
import com.example.chatservice.entities.Member;
import com.example.chatservice.entities.MemberChatroomMapping;
import com.example.chatservice.entities.Message;
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

    public Chatroom createChatroom(Member member, String title) {
        Chatroom chatroom = Chatroom.builder()
                .title(title)
                .createdAt(LocalDateTime.now())
                .build();

        chatroom = chatroomRepository.save(chatroom);

        MemberChatroomMapping memberChatroomMapping = chatroom.addMember(member);

        memberChatroomMapping = memberChatroomMappingRepository.save(memberChatroomMapping);

        return chatroom;
    }

    public Boolean joinChatroom(Member member, Long newChatroomId, Long currentChatroomId) {

        if(currentChatroomId != null) {
            updateLastCheckedAt(member, currentChatroomId);
        }

        if(memberChatroomMappingRepository.existsByMemberIdAndChatroomId(member.getId(), newChatroomId)) {
            log.info("이미 참여한 채팅방입니다.");
            return false;
        }
        Chatroom chatroom = chatroomRepository.findById(newChatroomId).get();

        MemberChatroomMapping memberChatroomMapping = MemberChatroomMapping.builder()
                .member(member)
                .chatroom(chatroom)
                .build();

        memberChatroomMapping = memberChatroomMappingRepository.save(memberChatroomMapping);

        return true;
    }

    private void updateLastCheckedAt(Member member, Long currentChatroomId) {
        MemberChatroomMapping memberChatroomMapping =
                memberChatroomMappingRepository.findByMemberIdAndChatroomId(member.getId(), currentChatroomId).get();
        memberChatroomMapping.updateLastCheckedAt();
        memberChatroomMappingRepository.save(memberChatroomMapping);

    }

    @Transactional
    public Boolean leaveChatroom(Member member, Long chatroomId) {
        if(!memberChatroomMappingRepository.existsByMemberIdAndChatroomId(member.getId(), chatroomId)){
            log.info("참여하지 않는 방입니다.");
            return false;
        }
        memberChatroomMappingRepository.deleteByMemberIdAndChatroomId(member.getId(), chatroomId);

        return true;
    }

    public List<Chatroom> getChatroomList(Member member) {
        List<MemberChatroomMapping> memberChatroomMappingList = memberChatroomMappingRepository
                .findAllByMemberId(member.getId());

        return memberChatroomMappingList.stream()
                .map(memberChatroomMapping -> {
                    Chatroom chatroom = memberChatroomMapping.getChatroom();
                    chatroom.setHasNewMessage(
                            messageRepository.existsByChatroomIdAndCreatedAtAfter(chatroom.getId(),
                                    memberChatroomMapping.getLastCheckedAt()));
                    return chatroom;
                })
                .toList();
    }

    public Message saveMessage(Member member, Long chatroomId, String text) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId).get();

        Message message = Message.builder()
                .text(text)
                .member(member)
                .chatroom(chatroom)
                .createdAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    public List<Message> getMessageList(Long chatroomId) {
        return messageRepository.findAllByChatroomId(chatroomId);
    }

    @Transactional(readOnly = true)
    public ChatroomDto getChatroom(Long chatroomId) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId).get();
        return ChatroomDto.from(chatroom);
    }
}
