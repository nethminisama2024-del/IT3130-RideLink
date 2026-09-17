package com.ridelink.account_service.dto;

import com.ridelink.account_service.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;
    
}
