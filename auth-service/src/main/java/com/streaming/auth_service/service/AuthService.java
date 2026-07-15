package com.streaming.auth_service.service;


import org.bouncycastle.jcajce.provider.asymmetric.ec.GMSignatureSpi.sha256WithSM2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streaming.auth_service.dto.RegisterResponse;
import com.streaming.auth_service.dto.LoginRequest;
import com.streaming.auth_service.dto.LoginResponse;
import com.streaming.auth_service.dto.RegisterRequest;
import com.streaming.auth_service.entity.AuthUser;
import com.streaming.auth_service.enums.Role;
import com.streaming.auth_service.event.AuthEventPublisher;
import com.streaming.auth_service.event.UserCreatedEvent;
import com.streaming.auth_service.exception.DuplicatedResourceException;
import com.streaming.auth_service.exception.InvalidInputException;
import com.streaming.auth_service.mapper.AuthUserMapper;
import com.streaming.auth_service.repository.AuthUserRepository;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUserMapper authUserMapper;
    private final JwtService jwtService;
    private final AuthEventPublisher authEventPublisher;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, AuthUserMapper authUserMapper, JwtService jwtService, AuthEventPublisher authEventPublisher) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authUserMapper = authUserMapper;
        this.jwtService = jwtService;
        this.authEventPublisher = authEventPublisher;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (authUserRepository.existsByUsername(request.username())) {
            throw new DuplicatedResourceException("username already exists");
        }

        if (authUserRepository.existsByEmail(request.email())) {
            throw new DuplicatedResourceException("email already exists");
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new InvalidInputException("passwords doesn't match");
        }

        AuthUser authUser = new AuthUser();
        authUser.setUsername(request.username());
        authUser.setEmail(request.email());
        authUser.setPasswordHash(passwordEncoder.encode(request.password()));
        authUser.setRole(Role.USER.toString());

        AuthUser saved = authUserRepository.save(authUser);
        authEventPublisher.userCreated(new UserCreatedEvent(saved.getId(), saved.getEmail(), saved.getUsername(), request.displayName(), saved.getCreatedAt()));

        return authUserMapper.toResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {

        AuthUser user = authUserRepository.findByEmail(request.email()).orElseThrow(() -> new InvalidInputException("invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidInputException("invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token);
    }

    public void deleteUser(Long userId) {
        authUserRepository.deleteById(userId);
    }
}
