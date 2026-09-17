package com.ridelink.account_service.service;

import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.enums.AccountStatus;
import com.ridelink.account_service.exception.ResourceNotFoundException;
import com.ridelink.account_service.repository.UserRepository;
import com.ridelink.account_service.dto.AccountResponse;
import com.ridelink.account_service.dto.UpdateProfileRequest;
import com.ridelink.account_service.dto.UpdateStatusRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AccountService {
    private final UserRepository userRepository;

    public AccountResponse getAccount(Long id) {

        User user = findUser(id);

        return toResponse(user);
    }

    public AccountResponse updateProfile(
            Long id,
            UpdateProfileRequest request) {

        User user = findUser(id);

        if (request.getName() != null &&
                !request.getName().isBlank()) {

            user.setName(request.getName());
        }

        if (request.getPhone() != null &&
                !request.getPhone().isBlank()) {

            user.setPhone(request.getPhone());
        }

        User updated =
                userRepository.save(user);

        return toResponse(updated);
    }

    public AccountResponse updateStatus(
            Long id,
            UpdateStatusRequest request) {

        User user = findUser(id);

        user.setStatus(request.getStatus());

        User updated =
                userRepository.save(user);

        return toResponse(updated);
    }

    private User findUser(Long id) {

        return userRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found: " + id
                        )
                );
    }

    private AccountResponse toResponse(User user) {

        return AccountResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
    
}
