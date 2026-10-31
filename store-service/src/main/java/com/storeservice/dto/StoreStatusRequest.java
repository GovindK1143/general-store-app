
package com.storeservice.dto;

import com.storeservice.model.StoreStatus;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreStatusRequest {

    @NotNull(message = "Store status is required")
    private StoreStatus status;
}
