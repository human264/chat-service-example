package com.example.chatservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Setter
@Entity
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

    // === 추가: 익명 채팅용 닉네임(별명) ===
    String aliasName;

    public void updateLastCheckedAt() {
        this.lastCheckedAt = LocalDateTime.now();
    }
}