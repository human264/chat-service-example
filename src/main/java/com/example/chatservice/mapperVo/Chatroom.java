package com.example.chatservice.mapperVo;


import com.example.chatservice.enums.ChatroomType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chatroom {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private Boolean hasNewMessage;
    private ChatroomType type;  // (ONE_TO_ONE, MULTI, ANONYMOUS)

    // 매퍼를 통해 채워질 멤버-채팅방 매핑 목록
    @Builder.Default
    private Set<MemberChatroomMapping> memberChatroomMappingSet = new HashSet<>();
    public void addMapping(MemberChatroomMapping mapping) {
        this.memberChatroomMappingSet.add(mapping);
    }

    public void removeMapping(Long memberId) {
        this.memberChatroomMappingSet.removeIf(m -> m.getMemberId().equals(memberId));
    }

    public int getMemberCount() {
        return memberChatroomMappingSet.size();
    }
}