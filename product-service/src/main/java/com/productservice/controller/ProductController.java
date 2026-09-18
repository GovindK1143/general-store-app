package com.productservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.productservice.model.Product;
import com.productservice.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/add")
    public ResponseEntity<Product> addProduct(
            @RequestBody Product product) {

        return ResponseEntity.ok(
                productService.addProduct(product)
        );
    }

    @GetMapping("/id/{productId}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                productService.getProductById(productId)
        );
    }

    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts() {

        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(category)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                productService.searchProducts(keyword)
        );
    }

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

    //Update Product
    @PutMapping("/{productId}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long productId,
            @RequestBody Product product) {

        return ResponseEntity.ok(
                productService.updateProduct(productId, product)
        );
    }

    //Activate/Deactivate Product
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