package com.example.chatservice.services;

import com.example.chatservice.dtos.ChatroomDto;
import com.example.chatservice.dtos.MemberDto;
import com.example.chatservice.mapper.ChatroomMapper;
import com.example.chatservice.mapper.MemberMapper;
import com.example.chatservice.mapperVo.Chatroom;
import com.example.chatservice.mapperVo.Member;

import com.example.chatservice.services.enums.Role;
import com.example.chatservice.vos.CustomerUserDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ConsultantService implements UserDetailsService {

    private final ChatroomMapper chatroomRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자가 없습니다."));

        if (Role.fromCode(member.getRole()) != Role.CONSULTANT) {
            try {
                throw new AccessDeniedException("상담사가 아닙니다.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
        return new CustomerUserDetail(member, null);
    }

    @Transactional
    public MemberDto saveMember(MemberDto memberDto) {
        Member member = MemberDto.to(memberDto);
        member.updatePassword(memberDto.password(), memberDto.confirmedPassword());
        member = memberRepository.save(member); // <-- default method (insert or update)
        return MemberDto.from(member);
    }

    @Transactional(readOnly = true)
    public List<ChatroomDto> getChatroomPage() {
        List<Chatroom> chatroomPage = chatroomRepository.findAll();
        return chatroomPage.stream().map(ChatroomDto::from).toList();
    }
}
