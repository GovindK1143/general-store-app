package com.storeservice.dto;

import java.time.LocalDateTime;

import com.storeservice.model.EmployeeDesignation;
import com.storeservice.model.EmploymentStatus;

public record StoreEmployeeResponse(
        Long id,
        Long userId,
        Long storeId,
        String storeCode,
        String storeName,
        String employeeCode,
        EmployeeDesignation designation,
        EmploymentStatus employmentStatus,
        LocalDateTime joinedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}