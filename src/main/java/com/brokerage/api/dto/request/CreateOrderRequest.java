package com.brokerage.api.dto.request;

import com.brokerage.domain.OrderSide;
import jakarta.validation.constraints.AssertTrue;
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
public class CreateOrderRequest {
    private static final String TRY_ASSET = "TRY";

    @NotNull(message = "Asset name must not be null")
    @NotBlank(message = "Asset name must not be empty")
    private String assetName;

    @NotNull(message = "Order side must not be null")
    private OrderSide orderSide;

    @NotNull(message = "Size must not be null")
    @DecimalMin(value = "0.0000001", message = "Size must be positive")
    private BigDecimal size;

    @NotNull(message = "Price must not be null")
    @DecimalMin(value = "0.0000001", message = "Price must be positive")
    private BigDecimal price;

    /**
     * Ensures TRY is not used as an asset in orders.
     */
    @AssertTrue(message = "TRY cannot be used as an asset in orders, TRY is used to buy/sell assets")
    public boolean isValidAsset() {
        return !TRY_ASSET.equalsIgnoreCase(assetName);
    }
}
