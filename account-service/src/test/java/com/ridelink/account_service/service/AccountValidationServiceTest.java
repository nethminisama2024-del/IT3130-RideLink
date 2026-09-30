package com.ridelink.account_service.service;

import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.enums.AccountStatus;
import com.ridelink.account_service.enums.Role;
import com.ridelink.account_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountValidationServiceTest {

    private UserRepository userRepository;
    private AccountValidationService accountValidationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accountValidationService = new AccountValidationService(userRepository);
    }

    @Test
    void shouldReturnTrueForActivePassenger() {
        User user = User.builder()
                .id(1L)
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertTrue(accountValidationService.isActivePassenger(1L));
    }

    @Test
    void shouldReturnFalseForInactivePassenger() {
        User user = User.builder()
                .id(2L)
                .role(Role.PASSENGER)
                .status(AccountStatus.INACTIVE)
                .build();

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        assertFalse(accountValidationService.isActivePassenger(2L));
    }

    @Test
    void shouldReturnFalseForDriverAccount() {
        User user = User.builder()
                .id(3L)
                .role(Role.DRIVER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        assertFalse(accountValidationService.isActivePassenger(3L));
    }

    @Test
    void shouldReturnFalseWhenAccountDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertFalse(accountValidationService.isActivePassenger(99L));
    }
}
