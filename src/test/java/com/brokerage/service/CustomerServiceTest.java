package com.brokerage.service;

import com.brokerage.domain.Customer;
import com.brokerage.exception.CustomerNotFoundException;
import com.brokerage.repository.CustomerRepository;
import com.brokerage.service.command.AssetCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AssetCommandService assetCommandService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private Customer adminCustomer;

    @BeforeEach
    void setUp() {
        // Set up test customer
        testCustomer = Customer.builder()
                .id(1L)
                .username("testuser")
                .password("encoded_password")
                .email("test@example.com")
                .fullName("Test User")
                .isAdmin(false)
                .build();

        // Set up admin customer
        adminCustomer = Customer.builder()
                .id(2L)
                .username("admin")
                .password("encoded_admin")
                .email("admin@example.com")
                .fullName("Admin User")
                .isAdmin(true)
                .build();
    }

    @Test
    void createCustomer_ShouldCreateCustomerWithInitialTryBalance() {
        when(customerRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        Customer result = customerService.createCustomer(
                "testuser", "password", "test@example.com", "Test User", false);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encoded_password", result.getPassword());
        assertEquals("test@example.com", result.getEmail());
        assertFalse(result.isAdmin());

        verify(customerRepository).existsByUsername("testuser");

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("testuser", capturedCustomer.getUsername());
        assertEquals("encoded_password", capturedCustomer.getPassword());
    }


    @Test
    void createCustomer_WhenUsernameExists_ShouldThrowException() {
        when(customerRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer(
                    "testuser", "password", "test@example.com", "Test User", false);
        });

        verify(customerRepository, never()).save(any(Customer.class));
        verify(assetCommandService, never()).createOrUpdateAsset(
                any(Customer.class), anyString(), any(BigDecimal.class));
    }

    @Test
    void updateCustomer_ShouldUpdateCustomerEmailAndFullName() {
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        Customer result = customerService.updateCustomer(
                1L, "updated@example.com", "Updated Name");

        assertNotNull(result);
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Updated Name", result.getFullName());

        verify(customerRepository).findById(1L);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("updated@example.com", capturedCustomer.getEmail());
        assertEquals("Updated Name", capturedCustomer.getFullName());
    }

    @Test
    void updateCustomer_WhenCustomerNotFound_ShouldThrowException() {
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.updateCustomer(1L, "updated@example.com", "Updated Name");
        });

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void changePassword_ShouldEncodeAndUpdatePassword() {
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        customerService.changePassword(1L, "newpassword");

        verify(customerRepository).findById(1L);
        verify(passwordEncoder).encode("newpassword");

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("new_encoded_password", capturedCustomer.getPassword());
    }

    @Test
    void getAllCustomers_ShouldReturnListOfCustomers() {
        when(customerRepository.findAll()).thenReturn(Arrays.asList(testCustomer, adminCustomer));

        List<Customer> result = customerService.getAllCustomers();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("admin", result.get(1).getUsername());

        verify(customerRepository).findAll();
    }

    @Test
    void getCustomerById_WhenCustomerExists_ShouldReturnCustomer() {
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());

        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerById_WhenCustomerNotFound_ShouldThrowException() {
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerById(1L);
        });

        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerByUsername_WhenCustomerExists_ShouldReturnCustomer() {
        when(customerRepository.findByUsername(anyString())).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.getCustomerByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(customerRepository).findByUsername("testuser");
    }

    @Test
    void getCustomerByUsername_WhenCustomerNotFound_ShouldThrowException() {
        when(customerRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerByUsername("nonexistent");
        });

        verify(customerRepository).findByUsername("nonexistent");
    }

    @Test
    void existsByUsername_WhenUsernameExists_ShouldReturnTrue() {
        when(customerRepository.existsByUsername(anyString())).thenReturn(true);

        boolean result = customerService.existsByUsername("testuser");

        assertTrue(result);

        verify(customerRepository).existsByUsername("testuser");
    }

    @Test
    void existsByUsername_WhenUsernameDoesNotExist_ShouldReturnFalse() {
        when(customerRepository.existsByUsername(anyString())).thenReturn(false);

        boolean result = customerService.existsByUsername("nonexistent");

        assertFalse(result);

        verify(customerRepository).existsByUsername("nonexistent");
    }
}