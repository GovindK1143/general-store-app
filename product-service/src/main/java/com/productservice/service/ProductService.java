package com.productservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.productservice.dto.StockUpdateBatchRequest;
import com.productservice.dto.StockUpdateItem;

import com.productservice.exception.InsufficientStockException;
import com.productservice.exception.ProductNotFoundException;
import com.productservice.model.Product;
import com.productservice.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;


    // =========================================================
    // ADD PRODUCT
    // =========================================================

    public Product addProduct(Product product) {

        if (product.getStock() == null) {
            product.setStock(0);
        }

        if (product.getActive() == null) {
            product.setActive(true);
        }

        validateProduct(product);

        return productRepository.save(product);
    }


    // =========================================================
    // GET ALL ACTIVE PRODUCTS
    // =========================================================

    public List<Product> getAllProducts() {

        return productRepository.findByActiveTrue();
    }


    // =========================================================
    // GET PRODUCTS BY CATEGORY
    // =========================================================

    public List<Product> getProductsByCategory(String category) {

        return productRepository
                .findByCategoryIgnoreCaseAndActiveTrue(category);
    }


    // =========================================================
    // SEARCH PRODUCTS
    // =========================================================

    public List<Product> searchProducts(String keyword) {

        return productRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(keyword);
    }


    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    public Product getProductById(Long productId) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + productId
                        )
                );

        // Customer should not be able to access inactive products
        if (!Boolean.TRUE.equals(product.getActive())) {

            throw new ProductNotFoundException(
                    "Product is currently unavailable"
            );
        }

        return product;
    }


    // =========================================================
    // UPDATE PRODUCT
    // =========================================================

    public Product updateProduct(
            Long productId,
            Product updatedProduct) {

        Product existingProduct = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + productId
                        )
                );


        // -----------------------------------------------------
        // Update Name
        // -----------------------------------------------------

        if (updatedProduct.getName() != null) {

            existingProduct.setName(
                    updatedProduct.getName().trim()
            );
        }


        // -----------------------------------------------------
        // Update Brand
        // -----------------------------------------------------

        if (updatedProduct.getBrand() != null) {

            existingProduct.setBrand(
                    updatedProduct.getBrand().trim()
            );
        }


        // -----------------------------------------------------
        // Update Category
        // -----------------------------------------------------

        if (updatedProduct.getCategory() != null) {

            existingProduct.setCategory(
                    updatedProduct.getCategory().trim()
            );
        }


        // -----------------------------------------------------
        // Update Description
        // -----------------------------------------------------

        if (updatedProduct.getDescription() != null) {

            existingProduct.setDescription(
                    updatedProduct.getDescription().trim()
            );
        }


        // -----------------------------------------------------
        // Update Image URL
        // -----------------------------------------------------

        if (updatedProduct.getImageUrl() != null) {

            existingProduct.setImageUrl(
                    updatedProduct.getImageUrl().trim()
            );
        }


        // -----------------------------------------------------
        // Update Unit
        // -----------------------------------------------------

        if (updatedProduct.getUnit() != null) {

            existingProduct.setUnit(
                    updatedProduct.getUnit().trim()
            );
        }


        // -----------------------------------------------------
        // Update MRP
        // -----------------------------------------------------

        if (updatedProduct.getMrp() != null) {

            existingProduct.setMrp(
                    updatedProduct.getMrp()
            );
        }


        // -----------------------------------------------------
        // Update Selling Price
        // -----------------------------------------------------

        if (updatedProduct.getSellingPrice() != null) {

            existingProduct.setSellingPrice(
                    updatedProduct.getSellingPrice()
            );
        }


        // -----------------------------------------------------
        // Update Stock
        // -----------------------------------------------------

        if (updatedProduct.getStock() != null) {

            existingProduct.setStock(
                    updatedProduct.getStock()
            );
        }


        // -----------------------------------------------------
        // Update Active Status
        // -----------------------------------------------------

        if (updatedProduct.getActive() != null) {

            existingProduct.setActive(
                    updatedProduct.getActive()
            );
        }


        // -----------------------------------------------------
        // Validate complete product
        // -----------------------------------------------------

        validateProduct(existingProduct);


        return productRepository.save(existingProduct);
    }


    // =========================================================
    // ACTIVATE / DEACTIVATE PRODUCT
    // =========================================================

    public Product updateProductStatus(
            Long productId,
            Boolean active) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + productId
                        )
                );


        if (active == null) {

            throw new IllegalArgumentException(
                    "Active status must be true or false"
            );
        }


        product.setActive(active);

        return productRepository.save(product);
    }


    // =========================================================
    // UPDATE / REDUCE STOCK
    // =========================================================

    public void updateStock(
            Long productId,
            int quantity) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + productId
                        )
                );


        // -----------------------------------------------------
        // Validate requested quantity
        // -----------------------------------------------------

        if (quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }


        // -----------------------------------------------------
        // Check available stock
        // -----------------------------------------------------

        if (product.getStock() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient stock. Available stock: "
                            + product.getStock()
            );
        }


        // -----------------------------------------------------
        // Reduce stock
        // -----------------------------------------------------

        product.setStock(
                product.getStock() - quantity
        );


        productRepository.save(product);
    }


    // =========================================================
    // PRODUCT VALIDATION
    // =========================================================

    private void validateProduct(Product product) {


        // -----------------------------------------------------
        // Name
        // -----------------------------------------------------

        if (product.getName() == null ||
                product.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Product name is required"
            );
        }


        // -----------------------------------------------------
        // Category
        // -----------------------------------------------------

        if (product.getCategory() == null ||
                product.getCategory().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Product category is required"
            );
        }


        // -----------------------------------------------------
        // MRP
        // -----------------------------------------------------

        if (product.getMrp() == null ||
                product.getMrp() < 0) {

            throw new IllegalArgumentException(
                    "MRP must be greater than or equal to zero"
            );
        }


        // -----------------------------------------------------
        // Selling Price
        // -----------------------------------------------------

        if (product.getSellingPrice() == null ||
                product.getSellingPrice() < 0) {

            throw new IllegalArgumentException(
                    "Selling price must be greater than or equal to zero"
            );
        }


        // -----------------------------------------------------
        // Selling Price cannot exceed MRP
        // -----------------------------------------------------

        if (product.getSellingPrice() > product.getMrp()) {

            throw new IllegalArgumentException(
                    "Selling price cannot be greater than MRP"
            );
        }


        // -----------------------------------------------------
        // Stock
        // -----------------------------------------------------

        if (product.getStock() == null ||
                product.getStock() < 0) {

            throw new IllegalArgumentException(
                    "Stock cannot be negative"
            );
        }


        // -----------------------------------------------------
        // Active
        // -----------------------------------------------------

        if (product.getActive() == null) {

            product.setActive(true);
        }
    }

    // =========================================================
// BATCH UPDATE / REDUCE STOCK
// =========================================================

    @Transactional
    public void updateStockBatch(
            StockUpdateBatchRequest request) {

        // -----------------------------------------------------
        // Validate request
        // -----------------------------------------------------

        if (request == null ||
                request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one stock update item is required"
            );
        }

        // -----------------------------------------------------
        // Prevent duplicate product IDs
        // -----------------------------------------------------

        Set<Long> productIds = new HashSet<>();

        for (StockUpdateItem item : request.getItems()) {

            if (item == null) {

                throw new IllegalArgumentException(
                        "Stock update item cannot be null"
                );
            }

            if (item.getProductId() == null) {

                throw new IllegalArgumentException(
                        "Product ID is required"
                );
            }

            if (item.getQuantity() == null ||
                    item.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Quantity must be greater than zero " +
                                "for product ID: " +
                                item.getProductId()
                );
            }

            if (!productIds.add(item.getProductId())) {

                throw new IllegalArgumentException(
                        "Duplicate product ID in stock update request: "
                                + item.getProductId()
                );
            }
        }

        // -----------------------------------------------------
        // IMPORTANT:
        // First validate ALL products and stock.
        // Do not reduce any stock yet.
        // -----------------------------------------------------

        List<Product> productsToUpdate =
                new ArrayList<>();

        for (StockUpdateItem item : request.getItems()) {

            Product product =
                    productRepository.findById(
                            item.getProductId()
                    ).orElseThrow(() ->
                            new ProductNotFoundException(
                                    "Product not found with ID: "
                                            + item.getProductId()
                            )
                    );

            if (!Boolean.TRUE.equals(product.getActive())) {

                throw new ProductNotFoundException(
                        "Product is currently unavailable: "
                                + item.getProductId()
                );
            }

            if (product.getStock() < item.getQuantity()) {

                throw new InsufficientStockException(
                        "Insufficient stock for product ID: "
                                + item.getProductId()
                                + ". Available stock: "
                                + product.getStock()
                );
            }

            productsToUpdate.add(product);
        }

        // -----------------------------------------------------
        // ALL products passed validation.
        // Now reduce stock.
        // -----------------------------------------------------

        for (int i = 0;
             i < request.getItems().size();
             i++) {

            StockUpdateItem item =
                    request.getItems().get(i);

            Product product =
                    productsToUpdate.get(i);

            product.setStock(
                    product.getStock()
                            - item.getQuantity()
            );
        }

        // -----------------------------------------------------
        // Save all updated products
        // -----------------------------------------------------

        productRepository.saveAll(productsToUpdate);
    }
}