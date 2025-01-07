package com.example.chatservice.mapper;

import com.example.chatservice.mapperVo.MemberChatroomMapping;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MemberChatroomMappingMapper {
    void insertMapping(MemberChatroomMapping mapping);
    boolean existsByMemberIdAndChatroomId(Long id, Long newChatroomId);

    Optional<MemberChatroomMapping> findByMemberIdAndChatroomId(Long id, Long chatroomId);

    void updateMapping(MemberChatroomMapping mapping);

    void deleteByMemberIdAndChatroomId(Long id, Long chatroomId);

    List<MemberChatroomMapping> findAllByChatroomId(Long chatroomId);

    List<MemberChatroomMapping> findAllByMemberId(Long id);
}
