package com.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;

    private String name;

    private String brand;

    private String category;

    private String description;

    private String imageUrl;

    private String unit;

    private Double mrp;

    private Double sellingPrice;

    private Integer stock;

    private Boolean active;
}