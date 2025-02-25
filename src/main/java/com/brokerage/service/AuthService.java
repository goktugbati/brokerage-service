package com.brokerage.service;

import com.brokerage.api.dto.request.CreateCustomerRequest;
import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.CustomerResponse;
import com.brokerage.api.dto.response.LoginResponse;
import com.brokerage.api.mapper.CustomerMapper;
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
    private final CustomerMapper customerMapper;

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

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Customer customer = customerService.getCustomerByUsername(userDetails.getUsername());

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (customer.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        User securityUser = new User(customer.getUsername(), customer.getPassword(), authorities);
        String token = jwtUtil.generateToken(securityUser);

        CustomerResponse customerResponse = customerMapper.toResponse(customer);

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

        return customerMapper.toResponse(customer);
    }
}