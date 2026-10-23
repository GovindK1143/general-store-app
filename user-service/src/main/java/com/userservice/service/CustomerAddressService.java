package com.userservice.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.userservice.dto.CustomerAddressRequest;
import com.userservice.dto.CustomerAddressResponse;
import com.userservice.model.CustomerAddress;
import com.userservice.model.User;
import com.userservice.repository.CustomerAddressRepository;
import com.userservice.repository.UserRepository;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final UserRepository userRepository;

    public CustomerAddressService(
            CustomerAddressRepository addressRepository,
            UserRepository userRepository) {

        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getMyAddresses(
            String email) {

        User user = getUser(email);

        return addressRepository
                .findAllByUser_IdOrderByDefaultAddressDescIdDesc(
                        user.getId()
                )
                .stream()
                .map(CustomerAddressResponse::from)
                .toList();
    }

    @Transactional
    public CustomerAddressResponse addAddress(
            String email,
            CustomerAddressRequest request) {

        User user = getUser(email);

        CustomerAddress address = new CustomerAddress();
        address.setUser(user);

        applyRequest(address, request);

        // The first saved address becomes the default.
        address.setDefaultAddress(
                !addressRepository.existsByUser_Id(user.getId())
        );

        return CustomerAddressResponse.from(
                addressRepository.save(address)
        );
    }

    @Transactional
    public CustomerAddressResponse updateAddress(
            String email,
            Long addressId,
            CustomerAddressRequest request) {

        User user = getUser(email);

        CustomerAddress address = getOwnedAddress(
                user.getId(),
                addressId
        );

        applyRequest(address, request);

        return CustomerAddressResponse.from(
                addressRepository.save(address)
        );
    }

    @Transactional
    public CustomerAddressResponse setDefaultAddress(
            String email,
            Long addressId) {

        User user = getUser(email);

        CustomerAddress selected = getOwnedAddress(
                user.getId(),
                addressId
        );

        List<CustomerAddress> addresses =
                addressRepository
                        .findAllByUser_IdOrderByDefaultAddressDescIdDesc(
                                user.getId()
                        );

        // Ensure only the selected address is the default.
        for (CustomerAddress address : addresses) {
            address.setDefaultAddress(
                    address.getId().equals(addressId)
            );
        }

        addressRepository.saveAll(addresses);

        return CustomerAddressResponse.from(selected);
    }

    @Transactional
    public void deleteAddress(
            String email,
            Long addressId) {

        User user = getUser(email);

        CustomerAddress address = getOwnedAddress(
                user.getId(),
                addressId
        );

        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);

        // Select another address if the default was removed.
        if (wasDefault) {
            List<CustomerAddress> remaining =
                    addressRepository
                            .findAllByUser_IdOrderByDefaultAddressDescIdDesc(
                                    user.getId()
                            );

            if (!remaining.isEmpty()) {
                CustomerAddress nextDefault = remaining.get(0);
                nextDefault.setDefaultAddress(true);
                addressRepository.save(nextDefault);
            }
        }
    }

    private User getUser(String email) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Customer account not found"
            );
        }

        return user;
    }

    private CustomerAddress getOwnedAddress(
            Long userId,
            Long addressId) {

        return addressRepository
                .findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Address not found"
                        )
                );
    }

    private void applyRequest(
            CustomerAddress address,
            CustomerAddressRequest request) {

        address.setLabel(request.label().trim());
        address.setRecipientName(request.recipientName().trim());
        address.setMobile(request.mobile().trim());
        address.setHouseFlat(request.houseFlat().trim());
        address.setStreetArea(request.streetArea().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPincode(request.pincode().trim());

        String landmark = request.landmark();

        address.setLandmark(
                landmark == null || landmark.isBlank()
                        ? null
                        : landmark.trim()
        );
    }
}