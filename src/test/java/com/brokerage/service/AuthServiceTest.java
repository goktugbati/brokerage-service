package com.brokerage.service;

import com.brokerage.api.dto.request.CreateCustomerRequest;
import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.CustomerResponse;
import com.brokerage.api.dto.response.LoginResponse;
import com.brokerage.api.mapper.CustomerMapper;
import com.brokerage.domain.Customer;
import com.brokerage.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomerService customerService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomerMapper customerMapper; // 🔹 FIX: Mock customerMapper

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private Customer testCustomer;
    private Authentication authentication;
    private UserDetails userDetails;
    private CreateCustomerRequest createCustomerRequest;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password")
                .build();

        testCustomer = Customer.builder()
                .id(1L)
                .username("testuser")
                .password("encoded_password")
                .email("test@example.com")
                .fullName("Test User")
                .isAdmin(false)
                .build();

        userDetails = new User(
                "testuser",
                "encoded_password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        authentication = mock(Authentication.class);

        createCustomerRequest = CreateCustomerRequest.builder()
                .username("newuser")
                .password("newpassword")
                .email("new@example.com")
                .fullName("New User")
                .build();
    }

    @Test
    void login_ShouldAuthenticateAndGenerateToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(customerService.getCustomerByUsername(anyString())).thenReturn(testCustomer);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt_token");

        when(customerMapper.toResponse(any(Customer.class)))
                .thenReturn(CustomerResponse.builder()
                        .id(testCustomer.getId())
                        .username(testCustomer.getUsername())
                        .email(testCustomer.getEmail())
                        .fullName(testCustomer.getFullName())
                        .admin(testCustomer.isAdmin())
                        .build());

        LoginResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("jwt_token", result.getToken());
        assertNotNull(result.getCustomer());
        assertEquals(testCustomer.getId(), result.getCustomer().getId());
        assertEquals(testCustomer.getUsername(), result.getCustomer().getUsername());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(any(UserDetails.class));
        verify(customerMapper).toResponse(testCustomer);
    }

    @Test
    void register_ShouldCreateNewCustomer() {
        when(customerService.createCustomer(
                eq("newuser"), eq("newpassword"), eq("new@example.com"), eq("New User"), eq(false)))
                .thenReturn(Customer.builder()
                        .id(2L)
                        .username("newuser")
                        .password("encoded_newpassword")
                        .email("new@example.com")
                        .fullName("New User")
                        .isAdmin(false)
                        .build());

        when(customerMapper.toResponse(any(Customer.class)))
                .thenReturn(CustomerResponse.builder()
                        .id(2L)
                        .username("newuser")
                        .email("new@example.com")
                        .fullName("New User")
                        .admin(false)
                        .build());

        CustomerResponse result = authService.register(createCustomerRequest);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New User", result.getFullName());
        assertFalse(result.isAdmin());

        verify(customerService).createCustomer(anyString(), anyString(), anyString(), anyString(), eq(false));
        verify(customerMapper).toResponse(any(Customer.class));
    }
}
