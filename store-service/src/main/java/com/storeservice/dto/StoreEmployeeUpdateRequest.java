package com.storeservice.dto;

import com.storeservice.model.EmployeeDesignation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreEmployeeUpdateRequest {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotBlank(message = "Employee code is required")
    @Size(max = 100, message = "Employee code cannot exceed 100 characters")
    private String employeeCode;

    @NotNull(message = "Designation is required")
    private EmployeeDesignation designation;
}