package com.example.chatservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MemberChatroomMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_chatroom_mapping_id")
    Long id;

    @JoinColumn(name = "member_id")
    @ManyToOne
    Member member;

    @JoinColumn(name = "chatroom_id")
    @ManyToOne
    Chatroom chatroom;

    LocalDateTime lastCheckedAt;

    // 익명 채팅 별명
    String aliasName;

    public void updateLastCheckedAt() {
        this.lastCheckedAt = LocalDateTime.now();
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }
}
