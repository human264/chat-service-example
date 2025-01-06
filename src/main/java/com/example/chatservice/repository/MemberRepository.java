package com.example.chatservice.repository;

import com.example.chatservice.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByName(String username);

    Optional<Member> findByEmail(String email);
}
