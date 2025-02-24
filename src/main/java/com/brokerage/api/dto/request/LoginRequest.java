package com.brokerage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotNull(message = "Username must not be null")
    @NotBlank(message = "Username must not be empty")
    private String username;

    @NotNull(message = "Password must not be null")
    @NotBlank(message = "Password must not be empty")
    private String password;
}
