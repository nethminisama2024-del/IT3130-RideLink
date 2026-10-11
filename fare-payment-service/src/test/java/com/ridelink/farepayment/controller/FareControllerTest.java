package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.exception.GlobalExceptionHandler;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.service.FareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FareControllerTest {

    @Mock
    private FareService fareService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FareController(fareService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void existingFareReturnsOkWithFareRecord() throws Exception {
        FareRecord record = new FareRecord();
        record.setId(7L);
        record.setRideId(1001L);
        record.setDistanceKm(5.0);
        record.setFareAmount(550.0);
        record.setPickupLocation("Colombo Fort");
        record.setDestination("Bambalapitiya");
        record.setCreatedAt(LocalDateTime.of(2026, 9, 17, 12, 0));
        when(fareService.getFareByRideId(1001L)).thenReturn(record);

        mockMvc.perform(get("/api/fares/ride/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.rideId").value(1001))
                .andExpect(jsonPath("$.distanceKm").value(5.0))
                .andExpect(jsonPath("$.fareAmount").value(550.0))
                .andExpect(jsonPath("$.pickupLocation").value("Colombo Fort"))
                .andExpect(jsonPath("$.destination").value("Bambalapitiya"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void missingFareReturnsNotFound() throws Exception {
        when(fareService.getFareByRideId(1001L))
                .thenThrow(new ResourceNotFoundException("Fare not found for ride ID: 1001"));

        mockMvc.perform(get("/api/fares/ride/1001"))
                .andExpect(status().isNotFound());
    }
}
