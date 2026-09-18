package com.ridelink.account_service.service;

import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.enums.*;
import com.ridelink.account_service.exception.*;
import com.ridelink.account_service.repository.UserRepository;
import com.ridelink.account_service.security.JwtService;
import com.ridelink.account_service.dto.AccountResponse;
import com.ridelink.account_service.dto.LoginRequest;
import com.ridelink.account_service.dto.LoginResponse;
import com.ridelink.account_service.dto.RegisterRequest;
import com.ridelink.account_service.enums.AccountStatus;
import com.ridelink.account_service.exception.AccountInactiveException;
import com.ridelink.account_service.exception.DuplicateEmailException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;  
    
    public AccountResponse registerPassenger(
            RegisterRequest request) {

        return register(request, Role.PASSENGER);
    }

    public AccountResponse registerDriver(
            RegisterRequest request) {

        return register(request, Role.DRIVER);
    }

    private AccountResponse register(
            RegisterRequest request,
            Role role) {

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new DuplicateEmailException(
                    "Email is already registered"
            );
        }

        User user = User.builder()
                .fullName(request.getName())
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .phone(request.getPhone())
                .role(role)
                .status(AccountStatus.ACTIVE)
                .build();

        User savedUser =
                userRepository.save(user);

        return toResponse(savedUser);
    }

    public LoginResponse login(
            LoginRequest request) {

        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid email or password"
                                )
                        );

        if (user.getStatus() ==
                AccountStatus.INACTIVE) {

            throw new AccountInactiveException(
                    "Account is inactive"
            );
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token =
                jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getRole()
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
