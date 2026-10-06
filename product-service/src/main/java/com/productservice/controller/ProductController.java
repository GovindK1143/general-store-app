package com.productservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.productservice.dto.StockUpdateBatchRequest;
import com.productservice.model.Product;
import com.productservice.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // =========================================================
    // ADD PRODUCT
    // =========================================================

    @PostMapping("/add")
    public ResponseEntity<Product> addProduct(
            @Valid @RequestBody Product product) {

        return ResponseEntity.ok(
                productService.addProduct(product)
        );
    }

    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    @GetMapping("/id/{productId}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                productService.getProductById(productId)
        );
    }

    // =========================================================
    // GET ALL ACTIVE PRODUCTS
    // =========================================================

    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts() {

        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    // =========================================================
    // GET PRODUCTS BY CATEGORY
    // =========================================================

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(category)
        );
    }

    // =========================================================
    // SEARCH PRODUCTS
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                productService.searchProducts(keyword)
        );
    }

    // =========================================================
    // UPDATE STOCK
    // =========================================================

    @PostMapping("/update-stock")
    public ResponseEntity<String> updateStock(
            @RequestBody Map<String, Object> request) {

        Long productId =
                Long.valueOf(request.get("productId").toString());

        int quantity =
                Integer.parseInt(request.get("quantity").toString());

        productService.updateStock(productId, quantity);

        return ResponseEntity.ok(
                "Stock updated successfully"
        );
    }

    // =========================================================
    // UPDATE STOCK - BATCH
    // =========================================================

    @PostMapping("/update-stock/batch")
    public ResponseEntity<String> updateStockBatch(
            @Valid @RequestBody StockUpdateBatchRequest request) {

        productService.updateStockBatch(request);

        return ResponseEntity.ok(
                "Stock updated successfully for all products"
        );
    }

    // =========================================================
    // UPDATE PRODUCT
    // =========================================================

    @PutMapping("/{productId}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long productId,
            @RequestBody Product product) {

        return ResponseEntity.ok(
                productService.updateProduct(productId, product)
        );
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE PRODUCT
    // =========================================================

    @PatchMapping("/{productId}/status")
    public ResponseEntity<Product> updateProductStatus(
            @PathVariable Long productId,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                productService.updateProductStatus(
                        productId,
                        active
                )
        );
    }
}