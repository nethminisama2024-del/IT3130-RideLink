package com.ridelink.account_service.service;

import com.ridelink.account_service.enums.AccountStatus;
import com.ridelink.account_service.enums.Role;
import com.ridelink.account_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountValidationService {

    private final UserRepository userRepository;

    public boolean isActivePassenger(Long accountId) {
        return userRepository.findById(accountId)
                .map(user ->
                        user.getStatus() == AccountStatus.ACTIVE
                        && user.getRole() == Role.PASSENGER
                )
                .orElse(false);
    }
}
