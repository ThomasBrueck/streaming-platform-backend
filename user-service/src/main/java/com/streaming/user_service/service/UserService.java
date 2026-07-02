package com.streaming.user_service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streaming.user_service.dto.CreateUserRequest;
import com.streaming.user_service.dto.UserResponse;
import com.streaming.user_service.entity.User;
import com.streaming.user_service.event.UserEventPublisher;
import com.streaming.user_service.exception.DuplicatedResourceException;
import com.streaming.user_service.exception.UserNotFoundException;
import com.streaming.user_service.mapper.UserMapper;
import com.streaming.user_service.repository.UserRepository;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher userEventPublisher;

    public UserService(UserRepository userRepository, UserMapper userMapper, UserEventPublisher userEventPublisher) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userEventPublisher = userEventPublisher;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("user not found: " + id));

        userRepository.delete(user);
        userEventPublisher.publishUserDeleted(id);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())){
            throw new DuplicatedResourceException("username already exists");
        }

        if (userRepository.existsByEmail(request.email())){
            throw new DuplicatedResourceException("email already taken");
        }

        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(userMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("user not found: " + id));

        return userMapper.toResponse(user);
    }
}
