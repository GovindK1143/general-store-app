
package com.inventoryservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.inventoryservice.client.ProductServiceClient;
import com.inventoryservice.client.StoreServiceClient;

import com.inventoryservice.dto.InventoryAvailabilityResponse;
import com.inventoryservice.dto.InventoryResponse;
import com.inventoryservice.dto.ProductServiceResponse;
import com.inventoryservice.dto.StockAdjustmentRequest;
import com.inventoryservice.dto.StoreServiceResponse;

import com.inventoryservice.exception.IdempotencyConflictException;

import com.inventoryservice.model.InventoryTransaction;
import com.inventoryservice.model.OutboxEvent;
import com.inventoryservice.model.StoreInventory;

import com.inventoryservice.model.enums.InventoryTransactionType;
import com.inventoryservice.model.enums.OutboxPublishStatus;

import com.inventoryservice.repository.InventoryTransactionRepository;
import com.inventoryservice.repository.OutboxEventRepository;
import com.inventoryservice.repository.StoreInventoryRepository;

import feign.FeignException;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final StoreInventoryRepository inventoryRepository;

    private final InventoryTransactionRepository transactionRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final StoreServiceClient storeServiceClient;

    private final ProductServiceClient productServiceClient;

    private final ObjectMapper objectMapper;

    private final EntityManager entityManager;

    public InventoryResponse adjustStock(
            Long storeId,
            Long productId,
            StockAdjustmentRequest request,
            Long actorUserId,
            String authorization) {

        validateAdjustmentRequest(request);

        // Check idempotency before performing external service calls.
        var existingTransaction =
                transactionRepository.findByIdempotencyKey(
                        request.idempotencyKey()
                );

        if (existingTransaction.isPresent()) {

            InventoryTransaction previous =
                    existingTransaction.get();

            boolean sameRequest =
                    previous.getStoreId().equals(storeId)
                            && previous.getProductId().equals(productId)
                            && previous.getTransactionType()
                            == request.transactionType()
                            && previous.getQuantityDelta()
                            .compareTo(request.quantityDelta()) == 0;

            if (!sameRequest) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used for a different stock operation"
                );
            }

            return getCurrentInventory(storeId, productId);
        }

        // Verify Store Service and Product Service records.
        verifyStore(storeId, authorization);

        ProductServiceResponse product =
                verifyProduct(productId, authorization);

        if (request.transactionType()
                == InventoryTransactionType.STOCK_RECEIVED
                && !Boolean.TRUE.equals(product.active())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot receive stock for an inactive product"
            );
        }

        BigDecimal initialThreshold =
                request.lowStockThreshold() == null
                        ? BigDecimal.ZERO
                        : request.lowStockThreshold();

        if (initialThreshold.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Low stock threshold cannot be negative"
            );
        }

        // Create a zero-stock row when this store/product pair
        // does not have an inventory record yet.
        inventoryRepository.insertIfAbsent(
                storeId,
                productId,
                initialThreshold
        );

        // Lock the inventory row before calculating the new quantity.
        StoreInventory inventory =
                inventoryRepository
                        .findByStoreIdAndProductIdForUpdate(
                                storeId,
                                productId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Unable to initialize inventory record"
                                )
                        );

        BigDecimal onHandBefore =
                inventory.getOnHandQuantity();

        BigDecimal reservedBefore =
                inventory.getReservedQuantity();

        BigDecimal onHandAfter =
                onHandBefore.add(request.quantityDelta());

        // Do not allow stock to become negative or fall below
        // the quantity already reserved for customers.
        if (onHandAfter.signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock adjustment would result in negative inventory"
            );
        }

        if (onHandAfter.compareTo(reservedBefore) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot reduce stock below the reserved quantity"
            );
        }

        // Update threshold only when explicitly supplied.
        if (request.lowStockThreshold() != null) {
            inventory.setLowStockThreshold(
                    request.lowStockThreshold()
            );
        }

        inventory.setOnHandQuantity(onHandAfter);

        inventoryRepository.saveAndFlush(inventory);

        // Refresh the MySQL-generated available_quantity column.
        entityManager.refresh(inventory);

        // Create immutable inventory audit transaction.
        InventoryTransaction transaction =
                new InventoryTransaction();

        transaction.setStoreId(storeId);
        transaction.setProductId(productId);

        transaction.setTransactionType(
                request.transactionType()
        );

        transaction.setQuantityDelta(
                request.quantityDelta()
        );

        transaction.setOnHandBefore(onHandBefore);
        transaction.setOnHandAfter(onHandAfter);

        transaction.setReservedBefore(reservedBefore);
        transaction.setReservedAfter(reservedBefore);

        transaction.setReferenceType(
                request.transactionType().name()
        );

        transaction.setActorUserId(actorUserId);
        transaction.setReason(request.reason());
        transaction.setIdempotencyKey(
                request.idempotencyKey()
        );

        transactionRepository.saveAndFlush(transaction);

        // Persist an event in the same database transaction.
        createOutboxEvent(
                inventory,
                transaction,
                request
        );

        return toInventoryResponse(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByStoreAndProduct(
            Long storeId,
            Long productId,
            String authorization) {

        verifyStore(storeId, authorization);
        verifyProduct(productId, authorization);

        return getCurrentInventory(storeId, productId);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByStore(
            Long storeId,
            String authorization) {

        verifyStore(storeId, authorization);

        List<StoreInventory> inventoryList =
                inventoryRepository
                        .findByStoreIdOrderByProductIdAsc(storeId);

        return inventoryList.stream()
                .map(this::toInventoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryAvailabilityResponse getAvailability(
            Long storeId,
            Long productId,
            String authorization) {

        verifyStore(storeId, authorization);

        ProductServiceResponse product =
                verifyProduct(productId, authorization);

        if (!Boolean.TRUE.equals(product.active())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product is inactive"
            );
        }

        var inventory =
                inventoryRepository.findByStoreIdAndProductId(
                        storeId,
                        productId
                );

        BigDecimal availableQuantity = inventory
                .map(StoreInventory::getAvailableQuantity)
                .orElse(BigDecimal.ZERO);

        BigDecimal threshold = inventory
                .map(StoreInventory::getLowStockThreshold)
                .orElse(BigDecimal.ZERO);

        return new InventoryAvailabilityResponse(
                storeId,
                productId,
                availableQuantity,
                availableQuantity.signum() > 0,
                availableQuantity.compareTo(threshold) <= 0
        );
    }

    private void validateAdjustmentRequest(
            StockAdjustmentRequest request) {

        if (request == null
                || request.quantityDelta() == null
                || request.transactionType() == null
                || request.idempotencyKey() == null
                || request.idempotencyKey().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stock adjustment request"
            );
        }

        InventoryTransactionType type =
                request.transactionType();

        BigDecimal delta = request.quantityDelta();

        if (delta.signum() == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity delta cannot be zero"
            );
        }

        if (type == InventoryTransactionType.STOCK_RECEIVED
                && delta.signum() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "STOCK_RECEIVED requires a positive quantity"
            );
        }

        if (type != InventoryTransactionType.STOCK_RECEIVED
                && type != InventoryTransactionType.STOCK_ADJUSTMENT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only STOCK_RECEIVED and STOCK_ADJUSTMENT are supported by this API"
            );
        }

        if (request.lowStockThreshold() != null
                && request.lowStockThreshold().signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Low stock threshold cannot be negative"
            );
        }
    }

    private StoreServiceResponse verifyStore(
            Long storeId,
            String authorization) {

        try {

            StoreServiceResponse store =
                    storeServiceClient.getStoreById(
                            storeId,
                            authorization
                    );

            if (store == null || store.id() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Store Service returned an invalid response"
                );
            }

            if (!"ACTIVE".equalsIgnoreCase(store.status())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Inventory operations are not allowed for an inactive store"
                );
            }

            return store;

        } catch (FeignException.NotFound exception) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Store not found: " + storeId
            );

        } catch (FeignException.Unauthorized exception) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Store Service rejected the authentication token"
            );

        } catch (FeignException.Forbidden exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "ADMIN authorization is required for this Store Service operation"
            );

        } catch (FeignException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to communicate with Store Service"
            );
        }
    }

    private ProductServiceResponse verifyProduct(
            Long productId,
            String authorization) {

        try {

            ProductServiceResponse product =
                    productServiceClient.getProductById(
                            productId,
                            authorization
                    );

            if (product == null || product.id() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Product Service returned an invalid response"
                );
            }

            return product;

        } catch (FeignException.NotFound exception) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found: " + productId
            );

        } catch (FeignException.Unauthorized exception) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Product Service rejected the authentication token"
            );

        } catch (FeignException.Forbidden exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied by Product Service"
            );

        } catch (FeignException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to communicate with Product Service"
            );
        }
    }

    private InventoryResponse getCurrentInventory(
            Long storeId,
            Long productId) {

        StoreInventory inventory =
                inventoryRepository
                        .findByStoreIdAndProductId(
                                storeId,
                                productId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Inventory record not found for store "
                                                + storeId
                                                + " and product "
                                                + productId
                                )
                        );

        return toInventoryResponse(inventory);
    }

    private InventoryResponse toInventoryResponse(
            StoreInventory inventory) {

        return new InventoryResponse(
                inventory.getId(),
                inventory.getStoreId(),
                inventory.getProductId(),
                inventory.getOnHandQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getLowStockThreshold(),
                inventory.getVersion(),
                inventory.getLastUpdated()
        );
    }

    private void createOutboxEvent(
            StoreInventory inventory,
            InventoryTransaction transaction,
            StockAdjustmentRequest request) {

        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("eventId", eventId);
        payload.put("eventType", "InventoryStockChanged");
        payload.put("storeId", inventory.getStoreId());
        payload.put("productId", inventory.getProductId());
        payload.put("transactionId", transaction.getId());
        payload.put("transactionType", request.transactionType().name());
        payload.put("quantityDelta", request.quantityDelta());
        payload.put("onHandQuantity", inventory.getOnHandQuantity());
        payload.put("reservedQuantity", inventory.getReservedQuantity());
        payload.put("availableQuantity", inventory.getAvailableQuantity());
        payload.put("occurredAt", LocalDateTime.now());

        OutboxEvent event = new OutboxEvent();

        event.setEventId(eventId);
        event.setAggregateType("STORE_INVENTORY");
        event.setAggregateId(inventory.getId());
        event.setEventType("InventoryStockChanged");

        event.setPayload(
                objectMapper.valueToTree(payload)
        );

        event.setPublishStatus(OutboxPublishStatus.PENDING);
        event.setRetryCount(0);

        outboxEventRepository.save(event);
    }
}
