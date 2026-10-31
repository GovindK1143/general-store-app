
package com.storeservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.storeservice.dto.StoreRequest;
import com.storeservice.dto.StoreResponse;
import com.storeservice.exception.DuplicateStoreCodeException;
import com.storeservice.exception.StoreNotFoundException;
import com.storeservice.model.Store;
import com.storeservice.model.StoreStatus;
import com.storeservice.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreResponse createStore(StoreRequest request) {

        validateRequest(request);

        String storeCode = normalizeStoreCode(request.getStoreCode());

        if (storeRepository.existsByStoreCodeIgnoreCase(storeCode)) {
            throw new DuplicateStoreCodeException(
                    "Store code already exists: " + storeCode
            );
        }

        Store store = new Store();

        copyRequestToEntity(request, store);

        // New stores remain inactive until explicitly activated.
        store.setStatus(StoreStatus.INACTIVE);

        return toResponse(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {

        return storeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {

        Store store = getStoreEntity(id);

        return toResponse(store);
    }

    public StoreResponse updateStore(Long id, StoreRequest request) {

        validateRequest(request);

        Store store = getStoreEntity(id);

        String storeCode = normalizeStoreCode(request.getStoreCode());

        storeRepository.findByStoreCodeIgnoreCase(storeCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateStoreCodeException(
                            "Store code already exists: " + storeCode
                    );
                });

        copyRequestToEntity(request, store);

        // Preserve current status.
        return toResponse(storeRepository.save(store));
    }

    public StoreResponse updateStoreStatus(
            Long id,
            StoreStatus status) {

        Store store = getStoreEntity(id);

        store.setStatus(status);

        return toResponse(storeRepository.save(store));
    }

    private Store getStoreEntity(Long id) {

        return storeRepository.findById(id)
                .orElseThrow(() ->
                        new StoreNotFoundException(
                                "Store not found with ID: " + id
                        )
                );
    }

    private void validateRequest(StoreRequest request) {

        boolean latitudePresent = request.getLatitude() != null;
        boolean longitudePresent = request.getLongitude() != null;

        if (latitudePresent != longitudePresent) {
            throw new IllegalArgumentException(
                    "Latitude and longitude must both be provided"
            );
        }

        if (Boolean.TRUE.equals(
                request.getOnlineFulfillmentEnabled())
                && (!latitudePresent || !longitudePresent)) {

            throw new IllegalArgumentException(
                    "Store coordinates are required for online fulfillment"
            );
        }

        if (request.getTimezone() == null ||
                request.getTimezone().isBlank()) {

            request.setTimezone("Asia/Kolkata");
        }
    }

    private String normalizeStoreCode(String storeCode) {

        return storeCode.trim().toUpperCase();
    }

    private void copyRequestToEntity(
            StoreRequest request,
            Store store) {

        store.setStoreCode(
                normalizeStoreCode(request.getStoreCode())
        );

        store.setStoreName(request.getStoreName().trim());
        store.setAddressLine1(request.getAddressLine1().trim());

        store.setAddressLine2(
                request.getAddressLine2() == null
                        ? null
                        : request.getAddressLine2().trim()
        );

        store.setCity(request.getCity().trim());
        store.setState(request.getState().trim());
        store.setPincode(request.getPincode().trim());

        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());

        store.setContactNumber(request.getContactNumber());

        store.setMaxDeliveryRadiusKm(
                request.getMaxDeliveryRadiusKm()
        );

        store.setOnlineFulfillmentEnabled(
                request.getOnlineFulfillmentEnabled()
        );

        store.setTimezone(request.getTimezone());

        store.setGstin(
                request.getGstin() == null ||
                        request.getGstin().isBlank()
                        ? null
                        : request.getGstin().trim().toUpperCase()
        );
    }

    private StoreResponse toResponse(Store store) {

        return new StoreResponse(
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                store.getAddressLine1(),
                store.getAddressLine2(),
                store.getCity(),
                store.getState(),
                store.getPincode(),
                store.getLatitude(),
                store.getLongitude(),
                store.getContactNumber(),
                store.getMaxDeliveryRadiusKm(),
                store.getOnlineFulfillmentEnabled(),
                store.getStatus(),
                store.getTimezone(),
                store.getGstin(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
