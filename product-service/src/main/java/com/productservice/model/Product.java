package com.productservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    @Column(length = 100)
    private String brand;

    @NotBlank(message = "Product category is required")
    @Size(max = 100, message = "Product category cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String category;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Column(length = 500)
    private String imageUrl;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    @Column(length = 50)
    private String unit;

    @PositiveOrZero(message = "MRP cannot be negative")
    @Column(nullable = false)
    private Double mrp;

    @PositiveOrZero(message = "Selling price cannot be negative")
    @Column(nullable = false)
    private Double sellingPrice;

    @PositiveOrZero(message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean active = true;
}