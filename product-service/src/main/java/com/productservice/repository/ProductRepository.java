package com.productservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.productservice.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findByCategoryIgnoreCase(String category);

	List<Product> findByActiveTrue();

	List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);

	List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

	// =========================================================
	// GET DISTINCT CATEGORIES CONTAINING ACTIVE PRODUCTS
	// =========================================================

	@Query("""
            SELECT DISTINCT p.category
            FROM Product p
            WHERE p.active = true
              AND p.category IS NOT NULL
              AND TRIM(p.category) <> ''
            ORDER BY LOWER(p.category), p.category
            """)
	List<String> findDistinctActiveCategories();

	// =========================================================
	// GET FEATURED PRODUCTS FOR QUICKMART HOMEPAGE
	// =========================================================

	List<Product> findByActiveTrueAndFeaturedTrueOrderByFeaturedOrderAscNameAsc();
}