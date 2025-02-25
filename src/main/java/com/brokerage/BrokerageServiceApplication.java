package com.brokerage;

import com.brokerage.domain.Customer;
import com.brokerage.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class BrokerageServiceApplication {

    private final CustomerService customerService;
    private final Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(BrokerageServiceApplication.class, args);
    }

    /**
     * Initialize admin user on application startup in dev environment
     */
//    @Bean
//    @Profile("!prod")
//    public CommandLineRunner initializeAdmin() {
//        return args -> {
//            log.info("Application running with profiles: {}", Arrays.toString(environment.getActiveProfiles()));
//
//            if (!customerService.existsByUsername("admin")) {
//                log.info("Creating default admin user");
//                Customer admin = customerService.createCustomer(
//                        "admin",
//                        "admin",
//                        "admin@brokerage.com",
//                        "System Administrator",
//                        true
//                );
//                log.info("Admin user created with ID: {}", admin.getId());
//            }
//
//            if (!customerService.existsByUsername("user")) {
//                log.info("Creating default regular user");
//                Customer user = customerService.createCustomer(
//                        "user",
//                        "password",
//                        "user@brokerage.com",
//                        "Regular User",
//                        false
//                );
//                log.info("Regular user created with ID: {}", user.getId());
//            }
//        };
//    }
}