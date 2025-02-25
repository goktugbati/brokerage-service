package com.brokerage.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddAssetRequest {
    @NotBlank(message = "Asset name must not be empty")
    private String assetName;

    @NotNull(message = "Initial size must not be null")
    @DecimalMin(value = "0.0000001", message = "Initial size must be positive")
    private BigDecimal initialSize;
}