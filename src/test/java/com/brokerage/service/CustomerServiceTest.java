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
        // Arrange
        when(customerRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        doNothing().when(assetCommandService).createOrUpdateAsset(
                any(Customer.class), anyString(), any(BigDecimal.class));

        // Act
        Customer result = customerService.createCustomer(
                "testuser", "password", "test@example.com", "Test User", false);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encoded_password", result.getPassword());
        assertEquals("test@example.com", result.getEmail());
        assertFalse(result.isAdmin());

        // Verify customer repository interactions
        verify(customerRepository).existsByUsername("testuser");

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("testuser", capturedCustomer.getUsername());
        assertEquals("encoded_password", capturedCustomer.getPassword());

        // Verify initial TRY balance
        verify(assetCommandService).createOrUpdateAsset(
                eq(testCustomer), eq("TRY"), eq(new BigDecimal("10000.00")));
    }

    @Test
    void createCustomer_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        when(customerRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer(
                    "testuser", "password", "test@example.com", "Test User", false);
        });

        // Verify no customer was saved
        verify(customerRepository, never()).save(any(Customer.class));
        verify(assetCommandService, never()).createOrUpdateAsset(
                any(Customer.class), anyString(), any(BigDecimal.class));
    }

    @Test
    void updateCustomer_ShouldUpdateCustomerEmailAndFullName() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        Customer result = customerService.updateCustomer(
                1L, "updated@example.com", "Updated Name");

        // Assert
        assertNotNull(result);
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Updated Name", result.getFullName());

        // Verify customer repository interactions
        verify(customerRepository).findById(1L);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("updated@example.com", capturedCustomer.getEmail());
        assertEquals("Updated Name", capturedCustomer.getFullName());
    }

    @Test
    void updateCustomer_WhenCustomerNotFound_ShouldThrowException() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.updateCustomer(1L, "updated@example.com", "Updated Name");
        });

        // Verify no customer was saved
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void changePassword_ShouldEncodeAndUpdatePassword() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        customerService.changePassword(1L, "newpassword");

        // Assert
        // Verify customer repository interactions
        verify(customerRepository).findById(1L);
        verify(passwordEncoder).encode("newpassword");

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("new_encoded_password", capturedCustomer.getPassword());
    }

    @Test
    void getAllCustomers_ShouldReturnListOfCustomers() {
        // Arrange
        when(customerRepository.findAll()).thenReturn(Arrays.asList(testCustomer, adminCustomer));

        // Act
        List<Customer> result = customerService.getAllCustomers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("admin", result.get(1).getUsername());

        // Verify customer repository interaction
        verify(customerRepository).findAll();
    }

    @Test
    void getCustomerById_WhenCustomerExists_ShouldReturnCustomer() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = customerService.getCustomerById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());

        // Verify customer repository interaction
        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerById_WhenCustomerNotFound_ShouldThrowException() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerById(1L);
        });

        // Verify customer repository interaction
        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerByUsername_WhenCustomerExists_ShouldReturnCustomer() {
        // Arrange
        when(customerRepository.findByUsername(anyString())).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = customerService.getCustomerByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        // Verify customer repository interaction
        verify(customerRepository).findByUsername("testuser");
    }

    @Test
    void getCustomerByUsername_WhenCustomerNotFound_ShouldThrowException() {
        // Arrange
        when(customerRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerByUsername("nonexistent");
        });

        // Verify customer repository interaction
        verify(customerRepository).findByUsername("nonexistent");
    }

    @Test
    void existsByUsername_WhenUsernameExists_ShouldReturnTrue() {
        // Arrange
        when(customerRepository.existsByUsername(anyString())).thenReturn(true);

        // Act
        boolean result = customerService.existsByUsername("testuser");

        // Assert
        assertTrue(result);

        // Verify customer repository interaction
        verify(customerRepository).existsByUsername("testuser");
    }

    @Test
    void existsByUsername_WhenUsernameDoesNotExist_ShouldReturnFalse() {
        // Arrange
        when(customerRepository.existsByUsername(anyString())).thenReturn(false);

        // Act
        boolean result = customerService.existsByUsername("nonexistent");

        // Assert
        assertFalse(result);

        // Verify customer repository interaction
        verify(customerRepository).existsByUsername("nonexistent");
    }
}