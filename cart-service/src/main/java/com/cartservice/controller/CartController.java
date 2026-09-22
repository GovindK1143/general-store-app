package com.cartservice.controller;

import com.cartservice.dto.AddCartItemRequest;
import com.cartservice.dto.CartResponse;
import com.cartservice.dto.UpdateCartItemRequest;
import com.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            Authentication authentication) {

        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                cartService.addItem(
                        userId,
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication) {

        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {

        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                cartService.updateItem(
                        userId,
                        productId,
                        request
                )
        );
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long productId,
            Authentication authentication) {

        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                cartService.removeItem(
                        userId,
                        productId
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<String> clearCart(
            Authentication authentication) {

        Long userId =
                getUserId(authentication);

        cartService.clearCart(userId);

        return ResponseEntity.ok(
                "Cart cleared successfully"
        );
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            Authentication authentication) {

        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                cartService.checkout(userId)
        );
    }

    private Long getUserId(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getDetails() == null) {

            throw new IllegalStateException(
                    "Authenticated user ID is missing"
            );
        }

        return (Long) authentication.getDetails();
    }
}