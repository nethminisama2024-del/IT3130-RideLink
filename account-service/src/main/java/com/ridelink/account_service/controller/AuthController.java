package com.ridelink.account_service.controller;

import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.service.AuthService;
import com.ridelink.account_service.dto.AccountResponse;
import com.ridelink.account_service.dto.LoginRequest;
import com.ridelink.account_service.dto.LoginResponse;
import com.ridelink.account_service.dto.RegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "Registration and login APIs"
)

public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/passenger")
    @Operation(summary = "Register a passenger")
    public ResponseEntity<AccountResponse>
    registerPassenger(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        authService.registerPassenger(
                                request
                        )
                );
    }

    @PostMapping("/register/driver")
    @Operation(summary = "Register a driver")
    public ResponseEntity<AccountResponse>
    registerDriver(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        authService.registerDriver(
                                request
                        )
                );
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT")
    public ResponseEntity<LoginResponse>
    login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
    
}
