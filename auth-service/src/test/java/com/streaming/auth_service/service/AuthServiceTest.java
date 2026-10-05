package com.streaming.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.streaming.auth_service.dto.LoginRequest;
import com.streaming.auth_service.dto.LoginResponse;
import com.streaming.auth_service.dto.RegisterRequest;
import com.streaming.auth_service.entity.AuthUser;
import com.streaming.auth_service.event.AuthEventPublisher;
import com.streaming.auth_service.exception.DuplicatedResourceException;
import com.streaming.auth_service.exception.InvalidInputException;
import com.streaming.auth_service.mapper.AuthUserMapper;
import com.streaming.auth_service.repository.AuthUserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthEventPublisher authEventPublisher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authUserRepository, passwordEncoder, new AuthUserMapper(), jwtService, authEventPublisher);
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(authUserRepository.existsByUsername("streamer")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("streamer", "a@b.com", null, "secret1", "secret1");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicatedResourceException.class);
    }

    @Test
    void register_rejectsMismatchedPasswords() {
        when(authUserRepository.existsByUsername(any())).thenReturn(false);
        when(authUserRepository.existsByEmail(any())).thenReturn(false);

        RegisterRequest request = new RegisterRequest("streamer", "a@b.com", null, "secret1", "different");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void register_savesHashedPasswordAndPublishesEvent() {
        when(authUserRepository.existsByUsername(any())).thenReturn(false);
        when(authUserRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(inv -> {
            AuthUser u = inv.getArgument(0);
            u.setId(1L);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        RegisterRequest request = new RegisterRequest("streamer", "a@b.com", "Display", "secret1", "secret1");
        authService.register(request);

        verify(authEventPublisher).userCreated(any());
    }

    @Test
    void login_rejectsWrongPassword() {
        AuthUser user = new AuthUser();
        user.setEmail("a@b.com");
        user.setPasswordHash("hashed");
        when(authUserRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
            .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void login_returnsTokenOnSuccess() {
        AuthUser user = new AuthUser();
        user.setEmail("a@b.com");
        user.setPasswordHash("hashed");
        when(authUserRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("right", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("a@b.com", "right"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void deleteUser_isIdempotentWhenAlreadyGone() {
        when(authUserRepository.existsById(5L)).thenReturn(false);

        authService.deleteUser(5L);

        verify(authUserRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_deletesWhenPresent() {
        when(authUserRepository.existsById(5L)).thenReturn(true);

        authService.deleteUser(5L);

        verify(authUserRepository).deleteById(5L);
    }
}
