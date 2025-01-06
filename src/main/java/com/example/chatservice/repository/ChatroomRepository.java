package com.example.chatservice.repository;

import com.example.chatservice.entities.Chatroom;
import org.hibernate.query.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;



public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
//    Page<Chatroom> findAll(Pageable pageable);

}
