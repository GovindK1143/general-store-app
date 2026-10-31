package com.storeservice.dto;

import java.time.LocalDateTime;

import com.storeservice.model.EmployeeDesignation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreEmployeeRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Employee code is required")
    @Size(max = 40)
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Employee code contains invalid characters"
    )
    private String employeeCode;

    @NotNull(message = "Designation is required")
    private EmployeeDesignation designation;

    private LocalDateTime joinedAt;
}