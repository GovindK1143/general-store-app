package com.userservice.dto;

import com.userservice.model.CustomerAddress;

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

    public static CustomerAddressResponse from(
            CustomerAddress address) {

        return new CustomerAddressResponse(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getMobile(),
                address.getHouseFlat(),
                address.getStreetArea(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getLandmark(),
                address.isDefaultAddress()
        );
    }
}