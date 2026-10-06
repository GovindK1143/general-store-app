package com.productservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateBatchRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotEmpty(message = "At least one stock update item is required")
    @Valid
    private List<StockUpdateItem> items;
}