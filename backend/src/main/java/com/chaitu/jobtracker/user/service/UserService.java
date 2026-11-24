package com.chaitu.jobtracker.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaitu.jobtracker.common.exception.ResourceNotFoundException;
import com.chaitu.jobtracker.user.dto.UserResponse;
import com.chaitu.jobtracker.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** readOnly = true tells Spring and the database that this method changes nothing. */
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
