package com.ridelink.account_service.dto;

import com.ridelink.account_service.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class LoginResponse {
    private String token;
    private Long accountId;
    private Role role;
}
