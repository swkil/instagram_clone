package com.example.backend.service;

import com.example.backend.dto.UserResponse;
import com.example.backend.dto.UserUpdateRequest;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.repository.FollowRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UserUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        boolean usernameChanged = !currentUser.getUsername().equals(request.getUsername());

        if (usernameChanged) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
            }
            currentUser.setUsername(request.getUsername());
        }

        currentUser.setFullName(request.getFullName());
        currentUser.setBio(request.getBio());

        if (request.getProfileImageUrl() != null) {
            currentUser.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(currentUser);

        UserResponse response = mapToUserResponse(updatedUser, updatedUser);

        if (usernameChanged) {
            String newAccessToken = jwtService.generateToken(updatedUser);
            String newRefreshToken = jwtService.generateRefreshToken(updatedUser);
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(newRefreshToken);
        }

        return response;
    }

    private UserResponse mapToUserResponse(User user) {
        User currentUser = authenticationService.getCurrentUser();

        return mapToUserResponse(user, currentUser);
    }

    private UserResponse mapToUserResponse(User user, User currentUser) {
        boolean isFollowing = false;
        if(!currentUser.getId().equals(user.getId())) {
            isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, user);
        }

        Long followersCount = followRepository.countFollowers(user);
        Long followingCount = followRepository.countFollowing(user);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .followersCount(followersCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .build();
    }
}