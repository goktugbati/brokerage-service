package com.brokerage.service;

import com.brokerage.api.dto.request.CreateCustomerRequest;
import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.CustomerResponse;
import com.brokerage.api.dto.response.LoginResponse;
import com.brokerage.domain.Customer;
import com.brokerage.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

        LoginResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("jwt_token", result.getToken());
        assertNotNull(result.getCustomer());
        assertEquals(testCustomer.getId(), result.getCustomer().getId());
        assertEquals(testCustomer.getUsername(), result.getCustomer().getUsername());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertEquals(loginRequest.getUsername(), tokenCaptor.getValue().getPrincipal());
        assertEquals(loginRequest.getPassword(), tokenCaptor.getValue().getCredentials());

        verify(jwtUtil).generateToken(any(UserDetails.class));
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

        CustomerResponse result = authService.register(createCustomerRequest);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New User", result.getFullName());
        assertFalse(result.isAdmin());

        verify(customerService).createCustomer(
                eq("newuser"), eq("newpassword"), eq("new@example.com"), eq("New User"), eq(false));
    }
    @Test
    void login_WhenInvalidCredentials_ShouldThrowAuthenticationException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid username or password"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(customerService, never()).getCustomerByUsername(anyString());
        verify(jwtUtil, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void register_WhenUsernameAlreadyExists_ShouldThrowException() {
        when(customerService.createCustomer(
                eq("newuser"), eq("newpassword"), eq("new@example.com"), eq("New User"), eq(false)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(createCustomerRequest);
        });

        verify(customerService).createCustomer(anyString(), anyString(), anyString(), anyString(), eq(false));
    }

    @Test
    void login_WhenCustomerNotFound_ShouldThrowException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(customerService.getCustomerByUsername(anyString()))
                .thenThrow(new com.brokerage.exception.CustomerNotFoundException("Customer not found"));

        assertThrows(com.brokerage.exception.CustomerNotFoundException.class, () -> {
            authService.login(loginRequest);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(customerService).getCustomerByUsername(anyString());
        verify(jwtUtil, never()).generateToken(any(UserDetails.class));
    }
}