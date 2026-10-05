package com.streaming.user_service.service;

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

import com.streaming.user_service.dto.UserResponse;
import com.streaming.user_service.entity.User;
import com.streaming.user_service.event.UserCreatedEvent;
import com.streaming.user_service.event.UserEventPublisher;
import com.streaming.user_service.exception.DuplicatedResourceException;
import com.streaming.user_service.exception.UnauthorizedMethod;
import com.streaming.user_service.exception.UserNotFoundException;
import com.streaming.user_service.mapper.UserMapper;
import com.streaming.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserEventPublisher userEventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper(), userEventPublisher);
    }

    private User sampleUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("streamer");
        user.setEmail("a@b.com");
        user.setDisplayName("Streamer");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void createUser_isIdempotentOnRedelivery() {
        User existing = sampleUser(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserCreatedEvent event = new UserCreatedEvent(1L, "a@b.com", "streamer", "Streamer", LocalDateTime.now());
        UserResponse response = userService.createUser(event);

        assertThat(response.id()).isEqualTo(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_rejectsDuplicateUsernameForDifferentId() {
        when(userRepository.existsById(2L)).thenReturn(false);
        when(userRepository.existsByUsername("streamer")).thenReturn(true);

        UserCreatedEvent event = new UserCreatedEvent(2L, "other@b.com", "streamer", null, LocalDateTime.now());

        assertThatThrownBy(() -> userService.createUser(event))
            .isInstanceOf(DuplicatedResourceException.class);
    }

    @Test
    void createUser_savesNewProfile() {
        when(userRepository.existsById(3L)).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserCreatedEvent event = new UserCreatedEvent(3L, "new@b.com", "newuser", "New User", LocalDateTime.now());
        UserResponse response = userService.createUser(event);

        assertThat(response.username()).isEqualTo("newuser");
    }

    @Test
    void deleteUser_rejectsNonOwnerNonAdmin() {
        assertThatThrownBy(() -> userService.deleteUser(1L, 2L, "USER"))
            .isInstanceOf(UnauthorizedMethod.class);
    }

    @Test
    void deleteUser_allowsOwner() {
        User user = sampleUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L, 1L, "USER");

        verify(userRepository).delete(user);
        verify(userEventPublisher).publishUserDeleted(1L);
    }

    @Test
    void deleteUser_allowsAdmin() {
        User user = sampleUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L, 99L, "ADMIN");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_notFoundThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(1L, 1L, "USER"))
            .isInstanceOf(UserNotFoundException.class);
    }
}
