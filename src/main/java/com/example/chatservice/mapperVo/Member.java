package com.example.chatservice.mapperVo;

import com.example.chatservice.enums.Gender;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    private Long id;
    private String email;
    private String nickName;
    private String password;
    private String name;
    private Gender gender;
    private String phoneNumber;
    private LocalDate birthDay;
    private String role;

    // 프로필 이미지 URL (또는 DB/Redis에 실제 이미지 바이트를 캐시할 수도 있음)
    private String profileImageUrl;

    public void updatePassword(String password, String confirmedPassword) {
        if (!password.equals(confirmedPassword)) {
            throw new IllegalArgumentException("패스워드 불일치");
        }
        // 실제로는 PasswordEncoder 등으로 암호화하는 로직을 별도 처리
        this.password = password;
    }
}