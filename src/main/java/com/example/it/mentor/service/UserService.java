package com.example.it.mentor.service;

import com.example.it.mentor.dto.UserInfoResponse;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.AuthMapper;
import com.example.it.mentor.repository.UserRepository;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmailAndDeletedFalse(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void linkAvatar(Long userId, Long fileId) {
        fileStorage.requireOwned(fileId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        user.setAvatarFileId(fileId);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return authMapper.toUserInfoResponse(findByEmail(email));
    }
}
