package com.brokerage.service;

import com.brokerage.domain.Customer;
import com.brokerage.exception.CustomerNotFoundException;
import com.brokerage.repository.CustomerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new customer
     */
    @Transactional
    public Customer createCustomer(String username, String password, String email, String fullName, boolean isAdmin) {
        if (customerRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        Customer customer = Customer.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .isAdmin(isAdmin)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        log.info("Created new customer: {}, isAdmin: {}", username, isAdmin);

        return savedCustomer;
    }

    /**
     * Updates an existing customer
     */
    @Transactional
    public Customer updateCustomer(Long customerId, String email, String fullName) {
        Customer customer = getCustomerById(customerId);

        customer.setEmail(email);
        customer.setFullName(fullName);

        Customer updatedCustomer = customerRepository.save(customer);

        log.info("Updated customer: {}", customer.getUsername());

        return updatedCustomer;
    }

    /**
     * Changes customer password
     */
    @Transactional
    public void changePassword(Long customerId, String newPassword) {
        Customer customer = getCustomerById(customerId);

        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        log.info("Changed password for customer: {}", customer.getUsername());
    }


    /**
     * Get all customers (admin only)
     */
    public List<Customer> getAllCustomers() {
        log.debug("Fetching all customers");
        return customerRepository.findAll();
    }

    /**
     * Get a customer by ID
     */
    public Customer getCustomerById(Long customerId) {
        log.debug("Fetching customer by ID: {}", customerId);
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
    }

    /**
     * Get a customer by username
     */
    public Customer getCustomerByUsername(String username) {
        log.debug("Fetching customer by username: {}", username);
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with username: " + username));
    }

    /**
     * Check if a username exists
     */
    public boolean existsByUsername(String username) {
        return customerRepository.existsByUsername(username);
    }
}