package com.brokerage.service;

import com.brokerage.api.dto.request.CreateCustomerRequest;
import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.CustomerResponse;
import com.brokerage.api.dto.response.LoginResponse;
import com.brokerage.domain.Customer;
import com.brokerage.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomerService customerService;
    private final JwtUtil jwtUtil;
    
    /**
     * Authenticate a user and generate JWT token
     */
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), 
                        loginRequest.getPassword()
                )
        );
        
        // Get authenticated user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // Find the customer in database
        Customer customer = customerService.getCustomerByUsername(userDetails.getUsername());
        
        // Generate JWT token
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (customer.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        User securityUser = new User(customer.getUsername(), customer.getPassword(), authorities);
        String token = jwtUtil.generateToken(securityUser);
        
        // Create response
        CustomerResponse customerResponse = CustomerResponse.builder()
                .id(customer.getId())
                .username(customer.getUsername())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .isAdmin(customer.isAdmin())
                .build();
        
        return LoginResponse.builder()
                .token(token)
                .customer(customerResponse)
                .build();
    }
    
    /**
     * Register a new customer
     */
    public CustomerResponse register(CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFullName(),
                false
        );
        
        // Create response
        return CustomerResponse.builder()
                .id(customer.getId())
                .username(customer.getUsername())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .isAdmin(customer.isAdmin())
                .build();
    }
}