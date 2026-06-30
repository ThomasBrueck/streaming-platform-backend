package com.streaming.auth_service.mapper;

import org.springframework.stereotype.Component;

import com.streaming.auth_service.dto.RegisterResponse;
import com.streaming.auth_service.entity.AuthUser;

@Component
public class AuthUserMapper {
    public RegisterResponse toResponse(AuthUser user) {
        return new RegisterResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        );
    }
}
