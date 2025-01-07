package com.example.chatservice.entities;


import com.example.chatservice.enums.ChatroomType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chatroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private Long id;

    private String title;
    private LocalDateTime createdAt;
    private Boolean hasNewMessage;

    @Enumerated(EnumType.STRING)
    private ChatroomType type;

    @OneToMany(mappedBy = "chatroom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberChatroomMapping> memberChatroomMappingSet = new HashSet<>();

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
