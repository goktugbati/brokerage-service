package com.brokerage.api.dto.request;

import com.brokerage.domain.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private String status;

    /**
     * Validates that if one date is provided, both are provided
     * and that startDate is before endDate
     */
    @AssertTrue(message = "If date range is specified, both startDate and endDate must be provided and startDate must be before endDate")
    public boolean isValidDateRange() {
        if (startDate == null && endDate == null) {
            return true;
        }

        if (startDate == null || endDate == null) {
            return false;
        }

        return startDate.isBefore(endDate);
    }

    /**
     * Validates that the status value is valid if provided
     */
    @AssertTrue(message = "Status must be one of: PENDING, MATCHED, CANCELED")
    public boolean isValidStatus() {
        if (status == null || status.isEmpty()) {
            return true;
        }

        try {
            OrderStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Convert status string to OrderStatus enum if present
     */
    public OrderStatus getOrderStatus() {
        return status != null && !status.isEmpty() ? OrderStatus.valueOf(status) : null;
    }
}