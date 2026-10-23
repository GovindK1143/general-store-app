package com.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerAddressRequest(

        @NotBlank
        @Size(max = 30)
        String label,

        @NotBlank
        @Size(max = 100)
        String recipientName,

        @NotBlank
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Mobile number must contain exactly 10 digits"
        )
        String mobile,

        @NotBlank
        @Size(max = 100)
        String houseFlat,

        @NotBlank
        @Size(max = 150)
        String streetArea,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 100)
        String state,

        @NotBlank
        @Pattern(
                regexp = "^[0-9]{6}$",
                message = "PIN code must contain exactly 6 digits"
        )
        String pincode,

        @Size(max = 150)
        String landmark

) {
}