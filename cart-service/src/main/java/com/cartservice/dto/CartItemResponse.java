package com.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long itemId;

    private Long productId;

    private String productName;

    private String brand;

    private String category;

    private String imageUrl;

    private String unit;

    private Double mrp;

    private Double sellingPrice;

    private Integer quantity;

    private Double subtotal;
}