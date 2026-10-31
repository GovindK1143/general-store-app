package com.storeservice.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record StoreOperatingHoursResponse(
        Long id,
        Long storeId,
        Byte dayOfWeek,
        String dayName,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean isClosed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}