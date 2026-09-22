package com.cartservice.service;

import com.cartservice.client.OrderServiceClient;
import com.cartservice.client.ProductServiceClient;
import com.cartservice.dto.*;
import com.cartservice.exception.CartException;
import com.cartservice.exception.CartNotFoundException;
import com.cartservice.model.Cart;
import com.cartservice.model.CartItem;
import com.cartservice.repository.CartItemRepository;
import com.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public CartResponse addItem(
            Long userId,
            AddCartItemRequest request) {

        validateUserId(userId);

        ProductResponse product =
                getProduct(request.getProductId());

        validateProduct(product);

        if (product.getStock() < request.getQuantity()) {
            throw new CartException(
                    "Insufficient stock for product: "
                            + product.getName()
                            + ". Available stock: "
                            + product.getStock()
            );
        }

        Cart cart = getOrCreateCart(userId);

        CartItem cartItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                request.getProductId()
                        )
                        .orElse(null);

        if (cartItem == null) {

            cartItem = new CartItem();

            cartItem.setProductId(
                    request.getProductId()
            );

            cartItem.setQuantity(
                    request.getQuantity()
            );

            cart.addItem(cartItem);

        } else {

            int newQuantity =
                    cartItem.getQuantity()
                            + request.getQuantity();

            if (newQuantity > product.getStock()) {
                throw new CartException(
                        "Requested quantity exceeds available stock. "
                                + "Available stock: "
                                + product.getStock()
                );
            }

            cartItem.setQuantity(newQuantity);
        }

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse getCart(Long userId) {

        validateUserId(userId);

        Cart cart =
                cartRepository.findByUserId(userId)
                        .orElseGet(() -> createEmptyCart(userId));

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(
            Long userId,
            Long productId,
            UpdateCartItemRequest request) {

        validateUserId(userId);

        ProductResponse product =
                getProduct(productId);

        validateProduct(product);

        if (request.getQuantity() > product.getStock()) {
            throw new CartException(
                    "Requested quantity exceeds available stock. "
                            + "Available stock: "
                            + product.getStock()
            );
        }

        Cart cart = getCartEntity(userId);

        CartItem item =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                productId
                        )
                        .orElseThrow(() ->
                                new CartException(
                                        "Product is not present in cart"
                                )
                        );

        item.setQuantity(request.getQuantity());

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(
            Long userId,
            Long productId) {

        validateUserId(userId);

        Cart cart = getCartEntity(userId);

        CartItem item =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                productId
                        )
                        .orElseThrow(() ->
                                new CartException(
                                        "Product is not present in cart"
                                )
                        );

        cart.removeItem(item);

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {

        validateUserId(userId);

        Cart cart = getCartEntity(userId);

        cart.getItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }

    @Transactional
    public Object checkout(Long userId) {

        validateUserId(userId);

        Cart cart = getCartEntity(userId);

        if (cart.getItems() == null ||
                cart.getItems().isEmpty()) {

            throw new CartException(
                    "Cannot checkout an empty cart"
            );
        }

        List<OrderItemRequest> orderItems =
                cart.getItems()
                        .stream()
                        .map(item ->
                                new OrderItemRequest(
                                        item.getProductId(),
                                        item.getQuantity()
                                )
                        )
                        .toList();

        CreateOrderRequest request =
                new CreateOrderRequest(orderItems);

        log.info(
                "Checking out cart. userId={}, cartId={}, itemCount={}",
                userId,
                cart.getId(),
                orderItems.size()
        );

        Map<String, Object> response =
                orderServiceClient.placeOrder(request);

        if (response == null || response.isEmpty()) {
            throw new CartException(
                    "Unable to create order"
            );
        }

        /*
         * Clear the cart only after Order Service
         * successfully accepts the order.
         */
        cart.getItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return response;
    }

    private Cart getOrCreateCart(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseGet(() ->
                        createEmptyCart(userId)
                );
    }

    private Cart createEmptyCart(Long userId) {

        LocalDateTime now =
                LocalDateTime.now();

        Cart cart = new Cart();

        cart.setUserId(userId);
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);
        cart.setItems(new ArrayList<>());

        return cartRepository.save(cart);
    }

    private Cart getCartEntity(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new CartNotFoundException(
                                "Cart not found for user: "
                                        + userId
                        )
                );
    }

    private ProductResponse getProduct(Long productId) {

        if (productId == null) {
            throw new CartException(
                    "Product ID is required"
            );
        }

        try {

            ProductResponse product =
                    productServiceClient
                            .getProductById(productId);

            if (product == null) {
                throw new CartException(
                        "Product not found with ID: "
                                + productId
                );
            }

            return product;

        } catch (CartException exception) {
            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Unable to retrieve product {}",
                    productId,
                    exception
            );

            throw new CartException(
                    "Unable to retrieve product information"
            );
        }
    }

    private void validateProduct(ProductResponse product) {

        if (!Boolean.TRUE.equals(product.getActive())) {

            throw new CartException(
                    "Product is currently unavailable: "
                            + product.getName()
            );
        }

        if (product.getSellingPrice() == null) {

            throw new CartException(
                    "Product price is not available"
            );
        }

        if (product.getStock() == null) {

            throw new CartException(
                    "Product stock information is not available"
            );
        }
    }

    private CartResponse buildCartResponse(Cart cart) {

        List<CartItemResponse> items =
                new ArrayList<>();

        double totalAmount = 0.0;

        int totalItems = 0;

        for (CartItem cartItem : cart.getItems()) {

            ProductResponse product =
                    getProduct(cartItem.getProductId());

            double subtotal =
                    product.getSellingPrice()
                            * cartItem.getQuantity();

            items.add(
                    new CartItemResponse(
                            cartItem.getId(),
                            product.getId(),
                            product.getName(),
                            product.getBrand(),
                            product.getCategory(),
                            product.getImageUrl(),
                            product.getUnit(),
                            product.getMrp(),
                            product.getSellingPrice(),
                            cartItem.getQuantity(),
                            subtotal
                    )
            );

            totalAmount += subtotal;

            totalItems += cartItem.getQuantity();
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    private void validateUserId(Long userId) {

        if (userId == null) {

            throw new CartException(
                    "Authenticated user ID is missing"
            );
        }
    }
}