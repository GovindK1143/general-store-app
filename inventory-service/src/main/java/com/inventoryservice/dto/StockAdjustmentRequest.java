
package com.inventoryservice.dto;

import java.math.BigDecimal;

import com.inventoryservice.model.enums.InventoryTransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(

        @NotNull(message = "Quantity delta is required")
        @Digits(
                integer = 9,
                fraction = 3,
                message = "Quantity supports up to 3 decimal places"
        )
        BigDecimal quantityDelta,

        @NotNull(message = "Transaction type is required")
        InventoryTransactionType transactionType,

        @Digits(
                integer = 9,
                fraction = 3,
                message = "Low stock threshold supports up to 3 decimal places"
        )
        BigDecimal lowStockThreshold,

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 120, message = "Idempotency key cannot exceed 120 characters")
        String idempotencyKey

) {
}
