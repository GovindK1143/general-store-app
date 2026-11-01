
package com.inventoryservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryResponse(

        Long id,
        Long storeId,
        Long productId,
        BigDecimal onHandQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal lowStockThreshold,
        Long version,
        LocalDateTime lastUpdated

) {
}
