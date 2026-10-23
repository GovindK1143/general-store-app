package com.orderservice.dto;

public record CustomerAddressResponse(
        Long id,
        String label,
        String recipientName,
        String mobile,
        String houseFlat,
        String streetArea,
        String city,
        String state,
        String pincode,
        String landmark,
        boolean defaultAddress
) {
}