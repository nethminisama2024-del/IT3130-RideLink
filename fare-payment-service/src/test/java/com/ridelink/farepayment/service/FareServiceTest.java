package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FinalFareRequest;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.repository.FareRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private FareRecordRepository fareRecordRepository;

    @InjectMocks
    private FareService fareService;

    private FareEstimateRequest estimateRequest;

    @BeforeEach
    void setUp() {

        estimateRequest = new FareEstimateRequest();
        estimateRequest.setPickup("Colombo Fort");
        estimateRequest.setDestination("Nugegoda");
        estimateRequest.setDistanceKm(10.0);
    }


    @Test
    @DisplayName("Should calculate fare correctly for 10 km")
    void testEstimateFare_10Km_Returns950() {
        // When
        FareEstimateResponse response = fareService.estimateFare(estimateRequest);

        // Then
        assertNotNull(response);
        assertEquals(10.0, response.getDistanceKm());
        assertEquals(950.0, response.getEstimatedFare()); // 150 + (10 × 80) = 950
        assertEquals("Colombo Fort", response.getPickup());
        assertEquals("Nugegoda", response.getDestination());
    }

    // ============================================
    // TEST 2:
    // ============================================
    @Test
    @DisplayName("Should calculate fare correctly for 5 km")
    void testEstimateFare_5Km_Returns550() {
        estimateRequest.setDistanceKm(5.0);

        FareEstimateResponse response = fareService.estimateFare(estimateRequest);

        assertEquals(550.0, response.getEstimatedFare()); // 150 + (5 × 80) = 550
    }

    // ============================================
    // TEST 3:
    // ============================================
    @Test
    @DisplayName("Should throw exception when distance is 0")
    void testEstimateFare_ZeroDistance_ThrowsException() {
        estimateRequest.setDistanceKm(0.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fareService.estimateFare(estimateRequest)
        );

        assertEquals("Distance must be positive", exception.getMessage());
    }

    // ============================================
    // TEST 4:
    // ============================================
    @Test
    @DisplayName("Should throw exception when distance is negative")
    void testEstimateFare_NegativeDistance_ThrowsException() {
        estimateRequest.setDistanceKm(-5.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> fareService.estimateFare(estimateRequest)
        );
    }

    // ============================================
    // TEST 5:
    // ============================================
    @Test
    @DisplayName("Should calculate final fare and save to database")
    void testCalculateFinalFare_SavesToDatabase() {
        // Given
        FinalFareRequest request = new FinalFareRequest();
        request.setRideId(1001L);
        request.setDistanceKm(10.0);
        request.setPickup("Colombo Fort");
        request.setDestination("Nugegoda");

        FareRecord savedRecord = new FareRecord();
        savedRecord.setId(1L);
        savedRecord.setRideId(1001L);
        savedRecord.setDistanceKm(10.0);
        savedRecord.setFareAmount(950.0);
        savedRecord.setPickupLocation("Colombo Fort");
        savedRecord.setDestination("Nugegoda");

        // Mock the repository
        when(fareRecordRepository.save(any(FareRecord.class))).thenReturn(savedRecord);

        // When
        FareRecord result = fareService.calculateFinalFare(request);

        // Then
        assertNotNull(result);
        assertEquals(1001L, result.getRideId());
        assertEquals(950.0, result.getFareAmount());
        assertEquals("Colombo Fort", result.getPickupLocation());

        // Verify that save() was called exactly once
        verify(fareRecordRepository, times(1)).save(any(FareRecord.class));
    }

    // ============================================
    // TEST 6: Final fare
    // ============================================
    @Test
    @DisplayName("Should throw exception for negative distance in final fare")
    void testCalculateFinalFare_NegativeDistance_ThrowsException() {
        FinalFareRequest request = new FinalFareRequest();
        request.setRideId(1001L);
        request.setDistanceKm(-10.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> fareService.calculateFinalFare(request)
        );

        // Verify save() was NEVER called
        verify(fareRecordRepository, never()).save(any(FareRecord.class));
    }
}