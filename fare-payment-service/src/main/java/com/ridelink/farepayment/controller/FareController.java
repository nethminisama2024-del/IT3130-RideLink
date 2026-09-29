package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FinalFareRequest;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fare", description = "Fare estimation and calculation APIs")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate fare for a trip")
    public ResponseEntity<FareEstimateResponse> estimateFare(
            @Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.estimateFare(request));
    }

    @PostMapping("/final")
    @Operation(summary = "Calculate final fare after ride completion")
    public ResponseEntity<FareRecord> calculateFinalFare(
            @Valid @RequestBody FinalFareRequest request) {
        return ResponseEntity.ok(fareService.calculateFinalFare(request));
    }
}