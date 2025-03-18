package com.productservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.productservice.model.Product;
import com.productservice.repository.ProductRepository;
@Service
public class ProductService {
	@Autowired
    private ProductRepository productRepository;

    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public void updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() >= quantity) {
            product.setStock(product.getStock() - quantity); // ✅ Reduce stock
            productRepository.save(product);
        } else {
            throw new RuntimeException("Insufficient stock");
        }
    }


}
