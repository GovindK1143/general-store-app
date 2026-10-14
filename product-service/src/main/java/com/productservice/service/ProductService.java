package com.productservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.productservice.dto.StockUpdateBatchRequest;
import com.productservice.dto.StockUpdateItem;
import com.productservice.exception.InsufficientStockException;
import com.productservice.exception.ProductNotFoundException;
import com.productservice.model.Product;
import com.productservice.model.StockUpdateStatus;
import com.productservice.model.StockUpdateTransaction;
import com.productservice.repository.ProductRepository;
import com.productservice.repository.StockUpdateTransactionRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockUpdateTransactionRepository stockUpdateTransactionRepository;

    @Autowired
    private CacheManager cacheManager;

    // =========================================================
    // ADD PRODUCT
    // =========================================================

    @Caching(evict = {
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productCategory", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public Product addProduct(Product product) {

        if (product.getStock() == null) {
            product.setStock(0);
        }

        if (product.getActive() == null) {
            product.setActive(true);
        }

        // Homepage configuration defaults.
        if (product.getFeatured() == null) {
            product.setFeatured(false);
        }

        if (product.getFeaturedOrder() == null) {
            product.setFeaturedOrder(0);
        }

        validateProduct(product);

        return productRepository.save(product);
    }

    // =========================================================
    // GET ALL ACTIVE PRODUCTS
    // =========================================================

    @Cacheable(value = "productList", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findByActiveTrue();
    }

    // =========================================================
    // GET DISTINCT CATEGORIES WITH ACTIVE PRODUCTS
    // Kept uncached until RedisConfig has been reviewed.
    // =========================================================

    public List<String> getActiveCategories() {
        return productRepository.findDistinctActiveCategories();
    }

    // =========================================================
    // GET FEATURED PRODUCTS FOR QUICKMART HOMEPAGE
    // Kept uncached until RedisConfig has been reviewed.
    // =========================================================

    public List<Product> getFeaturedProducts() {
        return productRepository
                .findByActiveTrueAndFeaturedTrueOrderByFeaturedOrderAscNameAsc();
    }

    // =========================================================
    // GET PRODUCTS BY CATEGORY
    // =========================================================

    @Cacheable(value = "productCategory", key = "#category.toLowerCase()")
    public List<Product> getProductsByCategory(String category) {
        return productRepository
                .findByCategoryIgnoreCaseAndActiveTrue(category);
    }

    // =========================================================
    // SEARCH PRODUCTS
    // =========================================================

    @Cacheable(
            value = "productSearch",
            key = "#keyword.trim().toLowerCase()"
    )
    public List<Product> searchProducts(String keyword) {
        return productRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(keyword);
    }

    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    @Cacheable(value = "products", key = "#productId")
    public Product getProductById(Long productId) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + productId
                ));

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

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productCategory", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public Product updateProduct(Long productId, Product updatedProduct) {

        Product existingProduct = productRepository
                .findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + productId
                ));

        if (updatedProduct.getName() != null) {
            existingProduct.setName(updatedProduct.getName().trim());
        }

        if (updatedProduct.getBrand() != null) {
            existingProduct.setBrand(updatedProduct.getBrand().trim());
        }

        if (updatedProduct.getCategory() != null) {
            existingProduct.setCategory(updatedProduct.getCategory().trim());
        }

        if (updatedProduct.getDescription() != null) {
            existingProduct.setDescription(updatedProduct.getDescription().trim());
        }

        if (updatedProduct.getImageUrl() != null) {
            existingProduct.setImageUrl(updatedProduct.getImageUrl().trim());
        }

        if (updatedProduct.getUnit() != null) {
            existingProduct.setUnit(updatedProduct.getUnit().trim());
        }

        if (updatedProduct.getMrp() != null) {
            existingProduct.setMrp(updatedProduct.getMrp());
        }

        if (updatedProduct.getSellingPrice() != null) {
            existingProduct.setSellingPrice(updatedProduct.getSellingPrice());
        }

        if (updatedProduct.getStock() != null) {
            existingProduct.setStock(updatedProduct.getStock());
        }

        if (updatedProduct.getActive() != null) {
            existingProduct.setActive(updatedProduct.getActive());
        }

        // Wrapper types (Boolean and Integer) let us distinguish an omitted
        // field from an explicit false or zero in an update request.
        if (updatedProduct.getFeatured() != null) {
            existingProduct.setFeatured(updatedProduct.getFeatured());
        }

        if (updatedProduct.getFeaturedOrder() != null) {
            existingProduct.setFeaturedOrder(updatedProduct.getFeaturedOrder());
        }

        validateProduct(existingProduct);

        return productRepository.save(existingProduct);
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE PRODUCT
    // =========================================================

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productCategory", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public Product updateProductStatus(Long productId, Boolean active) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + productId
                ));

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

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productCategory", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public void updateStock(Long productId, int quantity) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + productId
                ));

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        if (product.getStock() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available stock: " + product.getStock()
            );
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    // =========================================================
    // PRODUCT VALIDATION
    // =========================================================

    private void validateProduct(Product product) {

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category is required");
        }

        if (product.getMrp() == null || product.getMrp() < 0) {
            throw new IllegalArgumentException(
                    "MRP must be greater than or equal to zero"
            );
        }

        if (product.getSellingPrice() == null || product.getSellingPrice() < 0) {
            throw new IllegalArgumentException(
                    "Selling price must be greater than or equal to zero"
            );
        }

        if (product.getSellingPrice() > product.getMrp()) {
            throw new IllegalArgumentException(
                    "Selling price cannot be greater than MRP"
            );
        }

        if (product.getStock() == null || product.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        if (product.getActive() == null) {
            product.setActive(true);
        }

        if (product.getFeatured() == null) {
            product.setFeatured(false);
        }

        if (product.getFeaturedOrder() == null) {
            product.setFeaturedOrder(0);
        }

        if (product.getFeaturedOrder() < 0) {
            throw new IllegalArgumentException(
                    "Featured order cannot be negative"
            );
        }
    }

    // =========================================================
    // BATCH UPDATE / REDUCE STOCK WITH IDEMPOTENCY
    // =========================================================

    @Transactional
    public void updateStockBatch(StockUpdateBatchRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Stock update request is required");
        }

        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required for stock update");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one stock update item is required");
        }

        Long orderId = request.getOrderId();

        int claimed = stockUpdateTransactionRepository.claimOrder(
                orderId,
                LocalDateTime.now()
        );

        if (claimed == 0) {
            StockUpdateTransaction existingTransaction =
                    stockUpdateTransactionRepository
                            .findByOrderId(orderId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Stock update transaction already exists for orderId="
                                            + orderId
                            ));

            if (existingTransaction.getStatus() == StockUpdateStatus.COMPLETED) {
                return;
            }

            throw new IllegalStateException(
                    "Stock update is already being processed for orderId=" + orderId
            );
        }

        Set<Long> productIds = new HashSet<>();

        for (StockUpdateItem item : request.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Stock update item cannot be null");
            }

            if (item.getProductId() == null) {
                throw new IllegalArgumentException("Product ID is required");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero for productId="
                                + item.getProductId()
                );
            }

            if (!productIds.add(item.getProductId())) {
                throw new IllegalArgumentException(
                        "Duplicate product ID in stock update: " + item.getProductId()
                );
            }
        }

        List<Product> productsToUpdate = new ArrayList<>();

        for (StockUpdateItem item : request.getItems()) {
            Product product = productRepository
                    .findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product not found with ID: " + item.getProductId()
                    ));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new ProductNotFoundException(
                        "Product is currently unavailable: " + item.getProductId()
                );
            }

            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName()
                                + ". Available: " + product.getStock()
                                + ", requested: " + item.getQuantity()
                );
            }

            product.setStock(product.getStock() - item.getQuantity());
            productsToUpdate.add(product);
        }

        // Persist stock changes in the same transaction as the idempotency record.
        productRepository.saveAll(productsToUpdate);

        StockUpdateTransaction transaction = stockUpdateTransactionRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Stock transaction not found for orderId=" + orderId
                ));

        transaction.setStatus(StockUpdateStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        stockUpdateTransactionRepository.save(transaction);

        evictProductCaches(request.getItems());
    }

    // =========================================================
    // CACHE INVALIDATION AFTER BATCH STOCK UPDATE
    // =========================================================

    private void evictProductCaches(List<StockUpdateItem> items) {

        Cache productCache = cacheManager.getCache("products");
        if (productCache != null) {
            for (StockUpdateItem item : items) {
                productCache.evict(item.getProductId());
            }
        }

        // All products cache uses the key "all". Clear it so any additional
        // keys added to the cache later cannot leave stale catalogue data.
        Cache productListCache = cacheManager.getCache("productList");
        if (productListCache != null) {
            productListCache.clear();
        }

        Cache productCategoryCache = cacheManager.getCache("productCategory");
        if (productCategoryCache != null) {
            productCategoryCache.clear();
        }

        Cache productSearchCache = cacheManager.getCache("productSearch");
        if (productSearchCache != null) {
            productSearchCache.clear();
        }
    }
}
