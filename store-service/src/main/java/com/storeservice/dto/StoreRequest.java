
package com.storeservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreRequest {

    @NotBlank(message = "Store code is required")
    @Size(max = 30, message = "Store code cannot exceed 30 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Store code can contain letters, numbers, hyphens and underscores"
    )
    private String storeCode;

    @NotBlank(message = "Store name is required")
    @Size(max = 150)
    private String storeName;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 250)
    private String addressLine1;

    @Size(max = 250)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "PIN code is required")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "Enter a valid six-digit Indian PIN code"
    )
    private String pincode;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Digits(integer = 3, fraction = 7)
    private BigDecimal latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Digits(integer = 3, fraction = 7)
    private BigDecimal longitude;

    @Size(max = 20)
    private String contactNumber;

    @NotNull(message = "Delivery radius is required")
    @DecimalMin(
            value = "0.01",
            message = "Delivery radius must be greater than zero"
    )
    @Digits(integer = 6, fraction = 2)
    private BigDecimal maxDeliveryRadiusKm;

    @NotNull(message = "Online fulfillment setting is required")
    private Boolean onlineFulfillmentEnabled;

    @Size(max = 60)
    private String timezone = "Asia/Kolkata";

    @Pattern(
            regexp = "^(|[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z])$",
            message = "Invalid GSTIN format"
    )
    @Size(max = 20)
    private String gstin;
}
