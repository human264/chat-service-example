package com.example.chatservice.mapper;

import com.example.chatservice.mapperVo.Message;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageMapper {

    Boolean existsNewMessage(Long id, LocalDateTime lastCheckedAt);

    void insertMessage(Message msg);

    List<Message> findAllByChatroomId(Long chatroomId);
}
