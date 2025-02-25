package com.brokerage.api;

import com.brokerage.domain.Customer;
import com.brokerage.security.SecurityUser;
import com.brokerage.service.CustomerService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Helper class for extracting customer information from security context
 */
@Component
public class CustomerHelper {

    private final CustomerService customerService;

    public CustomerHelper(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Get customer ID from authenticated user details
     */
    public Long getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof SecurityUser) {
            return ((SecurityUser) userDetails).getCustomerId();
        }

        String username = userDetails.getUsername();
        Customer customer = customerService.getCustomerByUsername(username);
        return customer.getId();
    }
    /**
     * Get customer from authenticated user details
     */
    public Customer getCustomerFromUserDetails(UserDetails userDetails) {
        String username = userDetails.getUsername();
        return customerService.getCustomerByUsername(username);
    }
}