package com.brokerage.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class SecurityUser extends User {
    private final Long customerId;

    public SecurityUser(String username, String password, 
                        Collection<? extends GrantedAuthority> authorities,
                        Long customerId) {
        super(username, password, authorities);
        this.customerId = customerId;
    }
}
