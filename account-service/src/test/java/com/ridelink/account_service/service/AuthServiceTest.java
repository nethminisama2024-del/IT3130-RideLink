package com.ridelink.account_service.service;

import com.ridelink.account_service.dto.AccountResponse;
import com.ridelink.account_service.dto.RegisterRequest;
import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.enums.AccountStatus;
import com.ridelink.account_service.enums.Role;
import com.ridelink.account_service.repository.UserRepository;
import com.ridelink.account_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;


    /**
     * Test successful passenger registration.
     *
     * Verifies:
     * - email is checked
     * - password is encoded
     * - user is saved
     * - fullName is stored
     * - name is stored
     * - role is PASSENGER
     * - status is ACTIVE
     */
    @Test
    void registerPassenger_shouldPersistFullNameAndName() {

        // Arrange
        RegisterRequest request = new RegisterRequest();

        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("secret123");
        request.setPhone("1234567890");

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    user.setId(1L);

                    return user;
                });


        // Act
        AccountResponse response =
                authService.registerPassenger(request);


        // Capture the User object passed to repository.save()
        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(captor.capture());

        User savedUser = captor.getValue();


        // Assert - User details
        assertEquals(
                "Jane Doe",
                savedUser.getFullName()
        );

        assertEquals(
                "Jane Doe",
                savedUser.getName()
        );

        assertEquals(
                "jane@example.com",
                savedUser.getEmail()
        );

        assertEquals(
                "encoded-password",
                savedUser.getPassword()
        );

        assertEquals(
                "1234567890",
                savedUser.getPhone()
        );


        // Assert - Role and status
        assertEquals(
                Role.PASSENGER,
                savedUser.getRole()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                savedUser.getStatus()
        );


        // Assert - Response
        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "jane@example.com",
                response.getEmail()
        );

        assertEquals(
                Role.PASSENGER,
                response.getRole()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                response.getStatus()
        );


        // Verify password was encoded
        verify(passwordEncoder)
                .encode("secret123");


        // Verify email existence was checked
        verify(userRepository)
                .existsByEmail("jane@example.com");
    }


    /**
     * Test that the password is never stored as plain text.
     */
    @Test
    void registerPassenger_shouldEncodePassword() {

        // Arrange
        RegisterRequest request = new RegisterRequest();

        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("mypassword");
        request.setPhone("0712345678");

        when(userRepository.existsByEmail("john@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("mypassword"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    user.setId(2L);

                    return user;
                });


        // Act
        AccountResponse response =
                authService.registerPassenger(request);


        // Assert
        assertNotNull(response);

        verify(passwordEncoder)
                .encode("mypassword");

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(captor.capture());

        User savedUser = captor.getValue();


        // Password should be encoded
        assertEquals(
                "encoded-password",
                savedUser.getPassword()
        );

        // Plain password should NOT be stored
        assertNotEquals(
                "mypassword",
                savedUser.getPassword()
        );
    }


    /**
     * Test that a new passenger account is created
     * with the correct role and active status.
     */
    @Test
    void registerPassenger_shouldCreateActivePassenger() {

        // Arrange
        RegisterRequest request = new RegisterRequest();

        request.setName("Test Passenger");
        request.setEmail("passenger@example.com");
        request.setPassword("password123");
        request.setPhone("0771234567");

        when(userRepository.existsByEmail("passenger@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    user.setId(3L);

                    return user;
                });


        // Act
        AccountResponse response =
                authService.registerPassenger(request);


        // Assert
        assertNotNull(response);

        assertEquals(
                Role.PASSENGER,
                response.getRole()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                response.getStatus()
        );


        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(captor.capture());

        User savedUser = captor.getValue();


        assertEquals(
                Role.PASSENGER,
                savedUser.getRole()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                savedUser.getStatus()
        );
    }
}