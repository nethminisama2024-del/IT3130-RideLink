package com.ridelink.account_service.controller;

import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.service.AccountService;
import com.ridelink.account_service.dto.AccountResponse;
import com.ridelink.account_service.dto.UpdateProfileRequest;
import com.ridelink.account_service.dto.UpdateStatusRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(
    name = "Accounts",
    description = "Account profile and status management"
)
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{id}")
    @Operation(summary = "Get an account profile")
    public ResponseEntity<AccountResponse>
    getAccount(@PathVariable Long id) {

        return ResponseEntity.ok(
                accountService.getAccount(id)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an account profile")
    public ResponseEntity<AccountResponse>
    updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody
            UpdateProfileRequest request) {

        return ResponseEntity.ok(
                accountService.updateProfile(
                        id,
                        request
                )
        );
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate an account")
    public ResponseEntity<AccountResponse>
    updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody
            UpdateStatusRequest request) {

        return ResponseEntity.ok(
                accountService.updateStatus(
                        id,
                        request
                )
        );
    }
    
}
