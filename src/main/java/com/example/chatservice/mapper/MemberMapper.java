package com.example.chatservice.mapper;

import com.example.chatservice.mapperVo.Member;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MemberMapper {

    void insertMember(Member member);

    Optional<Member> findById(Long id);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByName(String name);

    void updateMember(Member member);

    List<Member> findAll(); // 필요 시

    default Member save(Member member) {
        if (member.getId() == null) {
            insertMember(member);
        } else {
            updateMember(member);
        }
        return member;
    }
}