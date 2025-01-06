package com.example.chatservice.services.enums;

import java.util.Arrays;

public enum Role {

    USER("ROLE_USER"), CONSULTANT("ROLE_CONSULTANT");
    String code;
    Role(String code) {
        this.code = code;
    }

    public String getCode () {
        return code;
    }

    public static Role fromCode(String code) {
        return Arrays.stream(Role.values())
                .filter(role -> role.getCode().equals(code))
                .findFirst()
                .orElseThrow();
    }


}
