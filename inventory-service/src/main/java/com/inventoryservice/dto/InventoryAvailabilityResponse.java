
package com.inventoryservice.dto;

import java.math.BigDecimal;

public record InventoryAvailabilityResponse(

        Long storeId,
        Long productId,
        BigDecimal availableQuantity,
        boolean available,
        boolean lowStock

) {
}
