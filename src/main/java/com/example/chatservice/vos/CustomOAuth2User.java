package com.example.chatservice.vos;

import com.example.chatservice.entities.Member;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class CustomOAuth2User  implements OAuth2User {
    Member member;
    public Member getMember() {
        return this.member;
    }
    Map<String, Object> attributeMap;
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributeMap;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> this.member.getRole());
    }
    @Override
    public String getName() {
        return this.member.getNickName();
    }
}
