
package com.storeservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.storeservice.dto.StoreOperatingHoursRequest;
import com.storeservice.dto.StoreOperatingHoursResponse;
import com.storeservice.service.StoreOperatingHoursService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/stores/{storeId}/operating-hours")
@RequiredArgsConstructor
@Validated
public class StoreOperatingHoursController {

    private final StoreOperatingHoursService operatingHoursService;

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreOperatingHoursResponse>>
    saveWeeklySchedule(
            @PathVariable Long storeId,
            @Valid @RequestBody
            List<@Valid StoreOperatingHoursRequest> requests) {

        return ResponseEntity.ok(
                operatingHoursService.saveWeeklySchedule(
                        storeId,
                        requests
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreOperatingHoursResponse>>
    getWeeklySchedule(@PathVariable Long storeId) {

        return ResponseEntity.ok(
                operatingHoursService.getWeeklySchedule(storeId)
        );
    }
}
