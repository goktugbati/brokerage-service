package com.brokerage.api;

import com.brokerage.api.dto.request.CreateCustomerRequest;
import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.ApiResponse;
import com.brokerage.api.dto.response.CustomerResponse;
import com.brokerage.api.dto.response.LoginResponse;
import com.brokerage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and generate JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request for user: {}", loginRequest.getUsername());
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", loginResponse));
    }
    
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> register(@Valid @RequestBody CreateCustomerRequest request) {
        log.info("Registration request for user: {}", request.getUsername());
        CustomerResponse customerResponse = authService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration successful", customerResponse));
    }
}