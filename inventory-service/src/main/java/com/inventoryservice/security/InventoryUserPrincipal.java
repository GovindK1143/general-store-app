
package com.inventoryservice.security;

public record InventoryUserPrincipal(
        Long userId,
        String email,
        String role
) {
}
