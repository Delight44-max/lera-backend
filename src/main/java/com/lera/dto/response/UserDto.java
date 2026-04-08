package com.lera.dto.response;

import com.lera.model.Role;
import com.lera.model.User;
import lombok.*;
import java.time.Instant;

@Data @Builder
public class UserDto {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private String fcmToken;
    private Instant createdAt;

    public static UserDto from(User u) {
        return UserDto.builder()
            .id(u.getId())
            .fullName(u.getFullName())
            .email(u.getEmail())
            .phoneNumber(u.getPhoneNumber())
            .role(u.getRole())
            .fcmToken(u.getFcmToken())
            .createdAt(u.getCreatedAt())
            .build();
    }
}
