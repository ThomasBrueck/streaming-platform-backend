package com.streaming.user_service.mapper;

import org.springframework.stereotype.Component;

import com.streaming.user_service.dto.UserResponse;
import com.streaming.user_service.entity.User;
import com.streaming.user_service.event.UserCreatedEvent;

@Component
public class UserMapper {

    public User toEntity(UserCreatedEvent request) {
        User user = new User();
        user.setId(request.userId());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getCreatedAt()
        );
    }
}
