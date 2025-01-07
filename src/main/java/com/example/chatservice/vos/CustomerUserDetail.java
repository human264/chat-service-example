package com.example.chatservice.vos;

import com.example.chatservice.entities.Member;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;



public class CustomerUserDetail extends CustomOAuth2User implements UserDetails {
    public CustomerUserDetail(Member member, Map<String, Object> attributeMap) {
        super(member, attributeMap);
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(member.getRole()));
    }

    @Override
    public String getPassword() {
        return this.member.getPassword();
    }

    @Override
    public String getUsername() {
        return this.member.getName();
    }
}
