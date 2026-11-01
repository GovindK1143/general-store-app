
package com.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductServiceResponse(
        Long id,
        String name,
        String unit,
        Boolean active
) {
}
