
package com.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StoreServiceResponse(
        Long id,
        String storeCode,
        String storeName,
        Boolean onlineFulfillmentEnabled,
        String status,
        String timezone
) {
}
