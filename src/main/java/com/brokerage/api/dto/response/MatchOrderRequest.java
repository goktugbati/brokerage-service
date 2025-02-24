package com.brokerage.api.dto.response;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchOrderRequest {
    @NotNull(message = "Order ID must not be null")
    private Long orderId;
}
