package com.userservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.userservice.dto.CustomerAddressRequest;
import com.userservice.dto.CustomerAddressResponse;
import com.userservice.service.CustomerAddressService;

@RestController
@RequestMapping("/users/me/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(
            CustomerAddressService addressService) {

        this.addressService = addressService;
    }

    @GetMapping
    public List<CustomerAddressResponse> getMyAddresses(
            Authentication authentication) {

        return addressService.getMyAddresses(
                authentication.getName()
        );
    }

    @PostMapping
    public ResponseEntity<CustomerAddressResponse> addAddress(
            Authentication authentication,
            @Valid @RequestBody CustomerAddressRequest request) {

        CustomerAddressResponse address =
                addressService.addAddress(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(address);
    }

    @PutMapping("/{addressId}")
    public CustomerAddressResponse updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @Valid @RequestBody CustomerAddressRequest request) {

        return addressService.updateAddress(
                authentication.getName(),
                addressId,
                request
        );
    }

    @PutMapping("/{addressId}/default")
    public CustomerAddressResponse setDefaultAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        return addressService.setDefaultAddress(
                authentication.getName(),
                addressId
        );
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        addressService.deleteAddress(
                authentication.getName(),
                addressId
        );

        return ResponseEntity.noContent().build();
    }
}