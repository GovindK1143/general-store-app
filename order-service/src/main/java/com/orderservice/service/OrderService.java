package com.orderservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.orderservice.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

import feign.FeignException;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.orderservice.client.UserServiceClient;
import com.orderservice.dto.CustomerAddressResponse;

import com.orderservice.client.PaymentServiceClient;
import com.orderservice.client.ProductServiceClient;
import com.orderservice.exception.InvalidOrderException;
import com.orderservice.exception.OrderNotFoundException;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.repository.OrderRepository;
import com.orderservice.dto.OrderItemResponse;
import com.orderservice.dto.OrderResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {

//    private static final String PAYMENT_SERVICE_CB =
//            "paymentServiceCB";

    private static final String ORDER_PENDING =
            "PENDING";

    private static final String ORDER_CONFIRMED =
            "CONFIRMED";

    private static final String ORDER_CANCELLED =
            "CANCELLED";

    private static final String PAYMENT_PENDING =
            "PENDING";

    private static final String PAYMENT_SUCCESS =
            "SUCCESS";

    private static final String PAYMENT_FAILED =
            "FAILED";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Autowired
    private PaymentProcessingService paymentProcessingService;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;


    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

        log.info("Fetching all orders");

        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::fromOrder)
                .toList();
    }


    // =========================================================
    // GET USER ORDERS
    // =========================================================

    @Cacheable(
            value = "userOrders",
            key = "'user:' + #userId",
            unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {

        if (userId == null) {
            throw new InvalidOrderException(
                    "User ID is required"
            );
        }

        log.info("Fetching orders for User ID: {}", userId);

        return orderRepository
                .findByUserIdOrderByOrderDateDesc(userId)
                .stream()
                .map(OrderResponse::fromOrder)
                .toList();
    }

    private CustomerAddressResponse getSelectedAddress(Long addressId) {

        if (addressId == null || addressId <= 0) {
            throw new InvalidOrderException(
                    "A valid delivery address ID is required"
            );
        }

        final List<CustomerAddressResponse> addresses;

        try {
            addresses = userServiceClient.getMyAddresses();

        } catch (FeignException exception) {

            log.error(
                    "Unable to validate delivery address through User Service. "
                            + "addressId={}, status={}",
                    addressId,
                    exception.status()
            );

            if (exception.status() == 401) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Unable to authenticate with User Service",
                        exception
                );
            }

            if (exception.status() == 403) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access denied while validating delivery address",
                        exception
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to validate the delivery address. Please try again.",
                    exception
            );
        }

        if (addresses == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "User Service returned no address information"
            );
        }

        return addresses.stream()
                .filter(address ->
                        address != null && addressId.equals(address.id())
                )
                .findFirst()
                .orElseThrow(() -> new InvalidOrderException(
                        "The selected delivery address does not belong to "
                                + "your account or no longer exists"
                ));
    }


    // =========================================================
    // PLACE MULTI-PRODUCT ORDER
    // =========================================================

    @CacheEvict(
            value = "userOrders",
            key = "'user:' + #userId"
    )
    @Transactional
    public OrderResponse placeOrder(
            CreateOrderRequest request,
            Long userId) {

        if (userId == null) {
            throw new InvalidOrderException(
                    "Authenticated user ID is missing"
            );
        }

        if (request == null
                || request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new InvalidOrderException(
                    "Order must contain at least one item"
            );
        }


        // ---------------------------------------------------------
        // VALIDATE ORDER ITEMS AND DUPLICATE PRODUCTS
        // ---------------------------------------------------------

        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequest itemRequest : request.getItems()) {

            if (itemRequest == null) {
                throw new InvalidOrderException(
                        "Order item is required"
                );
            }

            if (itemRequest.getProductId() == null) {
                throw new InvalidOrderException(
                        "Product ID is required"
                );
            }

            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {

                throw new InvalidOrderException(
                        "Quantity must be greater than zero"
                );
            }

            if (!productIds.add(itemRequest.getProductId())) {
                throw new InvalidOrderException(
                        "Duplicate product ID in order: "
                                + itemRequest.getProductId()
                );
            }
        }

        // Validate that the selected address belongs to the
        // authenticated customer through User Service.
        CustomerAddressResponse selectedAddress =
                getSelectedAddress(request.getAddressId());


        // ---------------------------------------------------------
        // CHECK FOR AN EXISTING MATCHING PENDING ORDER
        // ---------------------------------------------------------

        Order existingPendingOrder =
                findMatchingPendingOrder(
                        userId,
                        request.getItems(),
                        selectedAddress.id()
                );


        if (existingPendingOrder != null) {

            // Lock and reload the matching order before retrying.
            Order lockedExistingOrder = orderRepository
                    .findByIdAndUserIdForUpdate(
                            existingPendingOrder.getId(),
                            userId
                    )
                    .orElse(null);

            if (lockedExistingOrder != null) {

                // Recheck the latest status after acquiring the lock.
                if (ORDER_PENDING.equalsIgnoreCase(
                        lockedExistingOrder.getOrderStatus())
                        && PAYMENT_PENDING.equalsIgnoreCase(
                        lockedExistingOrder.getPaymentStatus())) {

                    log.warn(
                            "Matching pending order found. Retrying payment " +
                                    "using the existing order. orderId={}, userId={}",
                            lockedExistingOrder.getId(),
                            userId
                    );

                    return processPaymentAndUpdateOrder(
                            lockedExistingOrder
                    );
                }

                // Another request may have updated the order while we waited.
                log.info(
                        "Matching order was updated by another request. " +
                                "Returning the latest order state. orderId={}",
                        lockedExistingOrder.getId()
                );

                return OrderResponse.fromOrder(lockedExistingOrder);
            }
        }

        log.info(
                "Creating order. userId={}, itemCount={}",
                userId,
                request.getItems().size()
        );


        // ---------------------------------------------------------
        // CREATE NEW ORDER
        // ---------------------------------------------------------

        Order order = new Order();

        order.setUserId(userId);
        // Preserve delivery details independently of the saved
        // address, which the customer may edit later.
        order.setDeliveryAddressId(selectedAddress.id());
        order.setDeliveryAddressLabel(selectedAddress.label());
        order.setDeliveryRecipientName(selectedAddress.recipientName());
        order.setDeliveryMobile(selectedAddress.mobile());
        order.setDeliveryHouseFlat(selectedAddress.houseFlat());
        order.setDeliveryStreetArea(selectedAddress.streetArea());
        order.setDeliveryCity(selectedAddress.city());
        order.setDeliveryState(selectedAddress.state());
        order.setDeliveryPincode(selectedAddress.pincode());
        order.setDeliveryLandmark(selectedAddress.landmark());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(ORDER_PENDING);
        order.setPaymentStatus(PAYMENT_PENDING);
        order.setStockUpdated(false);
        order.setItems(new ArrayList<>());

        double totalAmount = 0.0;


        // ---------------------------------------------------------
        // VALIDATE PRODUCTS AND CREATE ORDER ITEMS
        // ---------------------------------------------------------

        for (OrderItemRequest itemRequest : request.getItems()) {

            log.info(
                    "Validating product. productId={}, quantity={}",
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );


            // Fetch the product from Product Service.
            ProductResponse product =
                    productServiceClient.getProductById(
                            itemRequest.getProductId()
                    );


            if (product == null) {
                throw new InvalidOrderException(
                        "Product not found with ID: "
                                + itemRequest.getProductId()
                );
            }


            // Validate product availability.
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new InvalidOrderException(
                        "Product is currently unavailable: "
                                + product.getName()
                );
            }

            if (product.getSellingPrice() == null) {
                throw new InvalidOrderException(
                        "Product selling price is not available: "
                                + product.getName()
                );
            }

            if (product.getStock() == null) {
                throw new InvalidOrderException(
                        "Product stock information is not available: "
                                + product.getName()
                );
            }

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InvalidOrderException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available stock: "
                                + product.getStock()
                );
            }


            // Calculate subtotal.
            double subtotal =
                    product.getSellingPrice()
                            * itemRequest.getQuantity();


            // Create order item.
            OrderItem orderItem = new OrderItem();

            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getSellingPrice());
            orderItem.setSubtotal(subtotal);

            order.addItem(orderItem);

            totalAmount += subtotal;
        }


        // ---------------------------------------------------------
        // SET ORDER TOTAL
        // ---------------------------------------------------------

        order.setTotalAmount(totalAmount);


        // ---------------------------------------------------------
        // SAVE ORDER AND ORDER ITEMS
        // ---------------------------------------------------------

        Order savedOrder = orderRepository.save(order);

        log.info(
                "Order created successfully. " +
                        "orderId={}, userId={}, totalAmount={}",
                savedOrder.getId(),
                userId,
                totalAmount
        );


        // ---------------------------------------------------------
        // PROCESS PAYMENT
        // ---------------------------------------------------------

        return processPaymentAndUpdateOrder(savedOrder);
    }

    // =========================================================
// RETRY PAYMENT FOR AN EXISTING ORDER
// =========================================================

    @CacheEvict(
            value = "userOrders",
            key = "'user:' + #userId"
    )
    @Transactional
    public OrderResponse retryPayment(
            Long orderId,
            Long userId) {

        // Validate order ID.
        if (orderId == null || orderId <= 0) {
            throw new InvalidOrderException(
                    "A valid order ID is required"
            );
        }

        // Validate authenticated customer.
        if (userId == null) {
            throw new InvalidOrderException(
                    "Authenticated user ID is missing"
            );
        }

        // Retrieve and lock the customer's order during payment retry.
        // This serializes concurrent retries for the same order.
        Order order = orderRepository
                .findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found with ID: " + orderId
                        )
                );

        String paymentStatus = order.getPaymentStatus();
        String orderStatus = order.getOrderStatus();

        // Do not process payment again for an already completed order.
        if (PAYMENT_SUCCESS.equalsIgnoreCase(paymentStatus)
                && Boolean.TRUE.equals(order.getStockUpdated())) {

            log.info(
                    "Order is already completed. Retry not required. orderId={}",
                    orderId
            );

            return OrderResponse.fromOrder(order);
        }

        // Allow retry when payment is pending.
        boolean pendingOrder =
                PAYMENT_PENDING.equalsIgnoreCase(paymentStatus)
                        && ORDER_PENDING.equalsIgnoreCase(orderStatus);

        // Allow retry when the previous payment failed.
        boolean failedOrder =
                PAYMENT_FAILED.equalsIgnoreCase(paymentStatus)
                        && ORDER_CANCELLED.equalsIgnoreCase(orderStatus);

        if (!pendingOrder && !failedOrder) {
            throw new InvalidOrderException(
                    "This order is not eligible for payment retry. " +
                            "Current order status: " + orderStatus +
                            ", payment status: " + paymentStatus
            );
        }

        log.info(
                "Retrying payment for existing order. orderId={}, userId={}",
                orderId,
                userId
        );

        // Reuse the existing order and payment-processing logic.
        // Do not create a new order or new order items.
        return processPaymentAndUpdateOrder(order);
    }


    // =========================================================
    // AUTOMATIC RECOVERY OF A DUE PENDING PAYMENT
    // =========================================================

    @CacheEvict(
            value = "userOrders",
            allEntries = true
    )
    @Transactional
    public boolean retryPendingPaymentIfDue(
            Long orderId,
            LocalDateTime cutoff) {

        if (orderId == null || cutoff == null) {
            throw new IllegalArgumentException(
                    "Order ID and retry cutoff are required"
            );
        }

        // Lock and reload the latest order state.
        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElse(null);

        if (order == null) {
            log.debug(
                    "Automatic payment recovery skipped: order not found. " +
                            "orderId={}",
                    orderId
            );
            return false;
        }

        // Recheck eligibility after acquiring the database lock.
        if (!ORDER_PENDING.equalsIgnoreCase(order.getOrderStatus())
                || !PAYMENT_PENDING.equalsIgnoreCase(
                order.getPaymentStatus())) {

            log.debug(
                    "Automatic payment recovery skipped: order is no longer " +
                            "pending. orderId={}, orderStatus={}, paymentStatus={}",
                    orderId,
                    order.getOrderStatus(),
                    order.getPaymentStatus()
            );

            return false;
        }

        LocalDateTime lastAttempt = order.getLastPaymentAttemptAt();

        // Exclude legacy orders and attempts that are not yet due.
        if (lastAttempt == null || !lastAttempt.isBefore(cutoff)) {
            return false;
        }

        log.info(
                "Starting automatic payment recovery. orderId={}, lastAttempt={}",
                orderId,
                lastAttempt
        );

        // Reuse the existing order and payment-processing path.
        processPaymentAndUpdateOrder(order);

        return true;
    }

    // =========================================================
    // FIND A MATCHING PENDING ORDER
    // =========================================================

    private Order findMatchingPendingOrder(
            Long userId,
            List<OrderItemRequest> requestedItems,
            Long requestedAddressId) {

        // Build a map of product ID -> requested quantity.
        Map<Long, Integer> requestedQuantities =
                new HashMap<>();

        for (OrderItemRequest item : requestedItems) {
            requestedQuantities.put(
                    item.getProductId(),
                    item.getQuantity()
            );
        }


        // Repository results are ordered newest first.
        List<Order> userOrders =
                orderRepository
                        .findByUserIdOrderByOrderDateDesc(userId);


        for (Order existingOrder : userOrders) {

            // Only retry orders whose order and payment are pending.
            if (!ORDER_PENDING.equalsIgnoreCase(
                    existingOrder.getOrderStatus())) {
                continue;
            }

            if (!PAYMENT_PENDING.equalsIgnoreCase(
                    existingOrder.getPaymentStatus())) {
                continue;
            }

            // Do not reuse an existing pending order if the
            // customer selected another delivery address.
            if (!Objects.equals(
                    existingOrder.getDeliveryAddressId(),
                    requestedAddressId)) {
                continue;
            }

            if (existingOrder.getItems() == null
                    || existingOrder.getItems().isEmpty()) {
                continue;
            }


            // Build a map of product ID -> previously ordered quantity.
            Map<Long, Integer> existingQuantities =
                    new HashMap<>();

            for (OrderItem existingItem :
                    existingOrder.getItems()) {

                existingQuantities.put(
                        existingItem.getProductId(),
                        existingItem.getQuantity()
                );
            }


            // Match only when both product IDs and quantities are equal.
            if (existingQuantities.equals(requestedQuantities)) {

                return existingOrder;
            }
        }

        return null;
    }


    // =========================================================
    // PROCESS PAYMENT AND UPDATE ORDER STATUS
    // =========================================================

    private OrderResponse processPaymentAndUpdateOrder(
            Order savedOrder) {

        // Record when this payment attempt begins.
        savedOrder.setLastPaymentAttemptAt(LocalDateTime.now());
        orderRepository.save(savedOrder);

        Map<String, Object> paymentResponse;

        try {

            paymentResponse =
                    paymentProcessingService.processPayment(savedOrder);

        } catch (Exception exception) {

            log.warn(
                    "Payment processing failed for orderId={}. " +
                            "Keeping payment PENDING. Reason: {}",
                    savedOrder.getId(),
                    exception.getMessage()
            );

            paymentResponse = new HashMap<>();

            paymentResponse.put(
                    "paymentStatus",
                    PAYMENT_PENDING
            );
        }


        // ---------------------------------------------------------
        // READ PAYMENT STATUS
        // ---------------------------------------------------------

        String paymentStatus =
                getStringValue(
                        paymentResponse,
                        "paymentStatus"
                );

        if (paymentStatus != null) {
            paymentStatus = paymentStatus.trim();
        }

        if (paymentStatus == null || paymentStatus.isEmpty()) {
            paymentStatus = PAYMENT_PENDING;
        }


        // ---------------------------------------------------------
        // PAYMENT SUCCESS
        // ---------------------------------------------------------

        if (PAYMENT_SUCCESS.equalsIgnoreCase(paymentStatus)) {

            savedOrder.setPaymentStatus(PAYMENT_SUCCESS);

            // Stock will be updated by the existing Kafka listener.
            savedOrder.setOrderStatus(ORDER_PENDING);
            savedOrder.setStockUpdated(false);

            orderRepository.save(savedOrder);

            log.info(
                    "Payment successful. Waiting for Kafka " +
                            "event to update stock. orderId={}",
                    savedOrder.getId()
            );


            // ---------------------------------------------------------
            // PAYMENT FAILED
            // ---------------------------------------------------------

        } else if (PAYMENT_FAILED.equalsIgnoreCase(paymentStatus)) {

            savedOrder.setPaymentStatus(PAYMENT_FAILED);
            savedOrder.setOrderStatus(ORDER_CANCELLED);
            savedOrder.setStockUpdated(false);

            orderRepository.save(savedOrder);

            log.warn(
                    "Payment failed. Order cancelled. orderId={}",
                    savedOrder.getId()
            );


            // ---------------------------------------------------------
            // PAYMENT PENDING
            // ---------------------------------------------------------

        } else {

            savedOrder.setPaymentStatus(PAYMENT_PENDING);
            savedOrder.setOrderStatus(ORDER_PENDING);
            savedOrder.setStockUpdated(false);

            orderRepository.save(savedOrder);

            log.info(
                    "Payment pending. orderId={}",
                    savedOrder.getId()
            );
        }


        return OrderResponse.fromOrder(savedOrder);
    }

    // =========================================================
    // GET ORDER STATUS
    // =========================================================

    public OrderStatusResponse getOrderStatus(
            Long orderId,
            Long userId,
            boolean admin) {

        Order order;

        if (admin) {

            order = orderRepository.findById(orderId)
                    .orElseThrow(() ->
                            new OrderNotFoundException(
                                    "Order not found with ID: "
                                            + orderId
                            )
                    );

        } else {

            order = orderRepository
                    .findByIdAndUserId(orderId, userId)
                    .orElseThrow(() ->
                            new OrderNotFoundException(
                                    "Order not found with ID: "
                                            + orderId
                            )
                    );
        }


        String transactionId = null;

        LocalDateTime paymentDate = null;

        String paymentStatus = order.getPaymentStatus();


        // ---------------------------------------------------------
        // GET LATEST PAYMENT INFORMATION
        // ---------------------------------------------------------

        try {

            Map<String, Object> payment =
                    paymentServiceClient.getPaymentByOrderId(
                            orderId
                    );

            if (payment != null && !payment.isEmpty()) {

                String servicePaymentStatus =
                        getStringValue(
                                payment,
                                "paymentStatus"
                        );

                if (servicePaymentStatus != null) {
                    paymentStatus = servicePaymentStatus;
                }

                transactionId =
                        getStringValue(
                                payment,
                                "transactionId"
                        );

                paymentDate =
                        getLocalDateTimeValue(
                                payment,
                                "paymentDate"
                        );
            }

        } catch (Exception exception) {

            log.warn(
                    "Unable to retrieve payment information " +
                            "for orderId={}: {}",
                    orderId,
                    exception.getMessage()
            );
        }


        return OrderStatusResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(
                        order.getItems()
                                .stream()
                                .map(OrderItemResponse::fromOrderItem)
                                .toList()
                )
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(paymentStatus)
                .transactionId(transactionId)
                .paymentDate(paymentDate)
                .build();
    }


    // =========================================================
    // MAP VALUE HELPERS
    // =========================================================

    private String getStringValue(
            Map<String, Object> data,
            String key) {

        if (data == null) {
            return null;
        }

        Object value = data.get(key);

        return value != null
                ? value.toString()
                : null;
    }


    private LocalDateTime getLocalDateTimeValue(
            Map<String, Object> data,
            String key) {

        if (data == null) {
            return null;
        }

        Object value = data.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        try {

            return LocalDateTime.parse(value.toString());

        } catch (Exception exception) {

            return null;
        }
    }
}