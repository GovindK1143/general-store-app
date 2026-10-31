package com.storeservice.dto;

import com.storeservice.model.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreEmployeeStatusRequest {

    @NotNull(message = "Employment status is required")
    private EmploymentStatus status;
}