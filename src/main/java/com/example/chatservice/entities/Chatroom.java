package com.example.chatservice.entities;


import com.example.chatservice.enums.ChatroomType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Chatroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private Long id;

    private String title;

    private LocalDateTime createdAt;

    private Boolean hasNewMessage;  // 새 메시지가 있는지 여부

    @Enumerated(EnumType.STRING)
    private ChatroomType type;      // 1:1, 일반, 익명 등

    // (mappedBy는 MemberChatroomMapping 쪽의 'chatroom' 필드와 동일해야 합니다.)
    @OneToMany(mappedBy = "chatroom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberChatroomMapping> memberChatroomMappingSet = new HashSet<>();

    // == 편의 메서드 ==
    public MemberChatroomMapping addMember(Member member) {
        MemberChatroomMapping mapping = MemberChatroomMapping.builder()
                .chatroom(this)
                .member(member)
                .build();

        this.memberChatroomMappingSet.add(mapping);
        return mapping;
    }

    public void removeMember(Member member) {
        memberChatroomMappingSet.removeIf(m -> m.getMember().getId().equals(member.getId()));
    }

    public int getMemberCount() {
        return this.memberChatroomMappingSet.size();
    }

}
