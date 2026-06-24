package com.streaming.user_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.streaming.user_service.dto.CreateUserRequest;
import com.streaming.user_service.dto.UserResponse;
import com.streaming.user_service.entity.User;
import com.streaming.user_service.mapper.UserMapper;
import com.streaming.user_service.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())){
            throw new IllegalArgumentException("username already taken");
        }

        if (userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("email already taken");
        }

        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    @Transactional
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(userMapper::toResponse)
            .toList();
    }

    @Transactional
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("user not found: " + id));

        return userMapper.toResponse(user);
    }
}
