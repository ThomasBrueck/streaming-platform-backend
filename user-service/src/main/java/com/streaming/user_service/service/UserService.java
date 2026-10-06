package com.streaming.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streaming.user_service.dto.UserResponse;
import com.streaming.user_service.entity.User;
import com.streaming.user_service.event.UserCreatedEvent;
import com.streaming.user_service.event.UserEventPublisher;
import com.streaming.user_service.exception.DuplicatedResourceException;
import com.streaming.user_service.exception.UnauthorizedMethod;
import com.streaming.user_service.exception.UserNotFoundException;
import com.streaming.user_service.mapper.UserMapper;
import com.streaming.user_service.repository.UserRepository;


@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher userEventPublisher;

    public UserService(UserRepository userRepository, UserMapper userMapper, UserEventPublisher userEventPublisher) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userEventPublisher = userEventPublisher;
    }

    @Transactional
    public void deleteUser(Long id, Long requestUserId, String requestUserRole) {
        if (!requestUserId.equals(id) && !requestUserRole.equals("ADMIN")) {
            throw new UnauthorizedMethod("you don't have permission to do this");
        }

        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("user not found: " + id));

        userRepository.delete(user);
        userEventPublisher.publishUserDeleted(id);
    }

    // main function: Listener for create user event from auth microservice
    // Kafka delivers "auth-events" at-least-once, so redelivery of the same
    // event must be a no-op instead of failing on the duplicate check below.
    @Transactional
    public UserResponse createUser(UserCreatedEvent request) {
        if (userRepository.existsById(request.userId())) {
            log.info("user profile {} already exists, skipping (idempotent)", request.userId());
            return userMapper.toResponse(userRepository.findById(request.userId()).orElseThrow());
        }

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
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("user not found: " + id));

        return userMapper.toResponse(user);
    }
}
