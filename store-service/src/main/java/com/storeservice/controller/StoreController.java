
package com.storeservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.storeservice.dto.StoreRequest;
import com.storeservice.dto.StoreResponse;
import com.storeservice.dto.StoreStatusRequest;
import com.storeservice.service.StoreService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreResponse> createStore(
            @Valid @RequestBody StoreRequest request) {

        StoreResponse response =
                storeService.createStore(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreResponse>> getAllStores() {

        return ResponseEntity.ok(
                storeService.getAllStores()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreResponse> getStoreById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                storeService.getStoreById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody StoreRequest request) {

        return ResponseEntity.ok(
                storeService.updateStore(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreResponse> updateStoreStatus(
            @PathVariable Long id,
            @Valid @RequestBody StoreStatusRequest request) {

        return ResponseEntity.ok(
                storeService.updateStoreStatus(
                        id,
                        request.getStatus()
                )
        );
    }
}
