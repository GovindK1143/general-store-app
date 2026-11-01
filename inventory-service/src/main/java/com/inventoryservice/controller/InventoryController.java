
package com.inventoryservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import com.inventoryservice.dto.InventoryAvailabilityResponse;
import com.inventoryservice.dto.InventoryResponse;
import com.inventoryservice.dto.StockAdjustmentRequest;

import com.inventoryservice.security.InventoryUserPrincipal;
import com.inventoryservice.service.InventoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // Receive stock or adjust stock for a specific store and product.
    @PostMapping("/stores/{storeId}/products/{productId}/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> adjustStock(

            @PathVariable Long storeId,

            @PathVariable Long productId,

            @Valid @RequestBody StockAdjustmentRequest request,

            @AuthenticationPrincipal InventoryUserPrincipal principal,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization) {

        InventoryResponse response =
                inventoryService.adjustStock(
                        storeId,
                        productId,
                        request,
                        principal.userId(),
                        authorization
                );

        return ResponseEntity.ok(response);
    }

    // Get all inventory items belonging to a store.
    @GetMapping("/stores/{storeId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getInventoryByStore(

            @PathVariable Long storeId,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization) {

        return ResponseEntity.ok(
                inventoryService.getInventoryByStore(
                        storeId,
                        authorization
                )
        );
    }

    // Get inventory for one product in a particular store.
    @GetMapping("/stores/{storeId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> getInventoryByProduct(

            @PathVariable Long storeId,

            @PathVariable Long productId,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization) {

        return ResponseEntity.ok(
                inventoryService.getInventoryByStoreAndProduct(
                        storeId,
                        productId,
                        authorization
                )
        );
    }

    // Get available stock for a particular product in a store.
    @GetMapping(
            "/stores/{storeId}/products/{productId}/availability"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryAvailabilityResponse>
    getAvailability(

            @PathVariable Long storeId,

            @PathVariable Long productId,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization) {

        return ResponseEntity.ok(
                inventoryService.getAvailability(
                        storeId,
                        productId,
                        authorization
                )
        );
    }
}
