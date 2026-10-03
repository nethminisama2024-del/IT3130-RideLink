package com.ridelink.account_service.dto;

import com.ridelink.account_service.enums.AccountStatus;
import com.ridelink.account_service.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

public class AccountResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private AccountStatus status;
    
}
