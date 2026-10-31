
package com.storeservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.storeservice.dto.StoreEmployeeRequest;
import com.storeservice.dto.StoreEmployeeResponse;
import com.storeservice.dto.StoreEmployeeUpdateRequest;
import com.storeservice.dto.StoreEmployeeStatusRequest;
import com.storeservice.service.StoreEmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StoreEmployeeController {

    private final StoreEmployeeService employeeService;

    // Assign employee to a store
    @PostMapping("/stores/{storeId}/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreEmployeeResponse> assignEmployee(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreEmployeeRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization) {

        StoreEmployeeResponse response =
                employeeService.assignEmployee(
                        storeId,
                        request,
                        authorization
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all employees assigned to a store
    @GetMapping("/stores/{storeId}/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreEmployeeResponse>>
    getEmployeesByStore(@PathVariable Long storeId) {

        return ResponseEntity.ok(
                employeeService.getEmployeesByStore(storeId)
        );
    }

    // Get employee assignment by ID
    @GetMapping("/stores/employees/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreEmployeeResponse> getEmployeeById(
            @PathVariable Long employeeId) {

        return ResponseEntity.ok(
                employeeService.getEmployeeById(employeeId)
        );
    }

    // Update existing employee assignment
    @PutMapping("/stores/employees/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreEmployeeResponse> updateEmployee(
            @PathVariable Long employeeId,
            @Valid @RequestBody StoreEmployeeUpdateRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization) {

        StoreEmployeeResponse response =
                employeeService.updateEmployee(
                        employeeId,
                        request,
                        authorization
                );

        return ResponseEntity.ok(response);
    }

    // Change employee employment status
    @PatchMapping("/stores/employees/{employeeId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreEmployeeResponse> updateEmploymentStatus(
            @PathVariable Long employeeId,
            @Valid @RequestBody StoreEmployeeStatusRequest request) {

        StoreEmployeeResponse response =
                employeeService.updateEmploymentStatus(
                        employeeId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}
