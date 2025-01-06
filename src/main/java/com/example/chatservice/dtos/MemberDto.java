package com.example.chatservice.dtos;

import com.example.chatservice.entities.Member;
import com.example.chatservice.enums.Gender;

import java.time.LocalDate;

public record MemberDto(
        Long id,
        String email,
        String nickName,
        String name,
        String password,
        String confirmedPassword,
        Gender gender,
        String phoneNumber,
        LocalDate birthday,
        String role
) {
    public static MemberDto from(Member member) {
        return new MemberDto(
                member.getId(),
                member.getEmail(),
                member.getNickName(),
                member.getName(),
                null,
        null,
                member.getGender(),
                member.getPhoneNumber(),
                member.getBirthDay(),
                member.getRole()
        );
    }
    public static Member to(MemberDto memberDto) {
        return Member.builder()
                .id(memberDto.id())
                .email(memberDto.email())
                .nickName(memberDto.nickName())
                .name(memberDto.name())
                .gender(memberDto.gender())
                .phoneNumber(memberDto.phoneNumber())
                .birthDay(memberDto.birthday())
                .role(memberDto.role())
                .build();
    }
}
