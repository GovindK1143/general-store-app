
package com.storeservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.storeservice.model.StoreStatus;

public record StoreResponse(
        Long id,
        String storeCode,
        String storeName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        BigDecimal latitude,
        BigDecimal longitude,
        String contactNumber,
        BigDecimal maxDeliveryRadiusKm,
        Boolean onlineFulfillmentEnabled,
        StoreStatus status,
        String timezone,
        String gstin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
