package com.example.chatservice.mapper;

import com.example.chatservice.mapperVo.Chatroom;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;
import java.util.Optional;

@Mapper
public interface ChatroomMapper {
    Chatroom findChatroomById(Long id);

    void insertChatroom(Chatroom chatroom);
    default Optional<Chatroom> findById(Long id) {
        return Optional.ofNullable(findChatroomById(id));
    }
    void deleteById(Long id);
    void updateChatroom(Chatroom chatroom);
    List<Chatroom> findAll();
}
