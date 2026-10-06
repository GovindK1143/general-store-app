package com.cartservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import feign.FeignException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartservice.client.ProductServiceClient;
import com.cartservice.dto.AddCartItemRequest;
import com.cartservice.dto.CartItemResponse;
import com.cartservice.dto.CartResponse;
import com.cartservice.dto.CreateOrderRequest;
import com.cartservice.dto.OrderItemRequest;
import com.cartservice.dto.ProductResponse;
import com.cartservice.dto.UpdateCartItemRequest;
import com.cartservice.exception.CartException;
import com.cartservice.exception.CartNotFoundException;
import com.cartservice.model.Cart;
import com.cartservice.model.CartItem;
import com.cartservice.repository.CartItemRepository;
import com.cartservice.repository.CartRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final ProductServiceClient productServiceClient;

    private final OrderCheckoutService orderCheckoutService;


    /*
     * ============================================================
     * ADD ITEM
     * ============================================================
     *
     * After successfully adding/updating an item, the returned
     * CartResponse is written directly into Redis.
     *
     * Redis key:
     * carts::user:2
     */
    @Transactional
    @CachePut(
            value = "carts",
            key = "'user:' + #userId"
    )
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


    /*
     * ============================================================
     * GET CART
     * ============================================================
     *
     * First request:
     *     Redis MISS
     *     -> MySQL
     *     -> Product Service
     *     -> response stored in Redis
     *
     * Next request:
     *     Redis HIT
     *     -> MySQL is not queried
     *     -> Product Service is not called
     */
    @Transactional
    @Cacheable(
            value = "carts",
            key = "'user:' + #userId"
    )
    public CartResponse getCart(Long userId) {

        validateUserId(userId);

        log.info(
                "Redis cache MISS for cart. userId={}",
                userId
        );

        Cart cart =
                cartRepository.findByUserId(userId)
                        .orElseGet(
                                () -> createEmptyCart(userId)
                        );

        return buildCartResponse(cart);
    }


    /*
     * ============================================================
     * UPDATE ITEM
     * ============================================================
     *
     * Returned CartResponse replaces the old Redis value.
     */
    @Transactional
    @CachePut(
            value = "carts",
            key = "'user:' + #userId"
    )
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
                        .orElseThrow(
                                () -> new CartException(
                                        "Product is not present in cart"
                                )
                        );

        item.setQuantity(
                request.getQuantity()
        );

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return buildCartResponse(cart);
    }


    /*
     * ============================================================
     * REMOVE ITEM
     * ============================================================
     *
     * Returned CartResponse replaces the old Redis value.
     */
    @Transactional
    @CachePut(
            value = "carts",
            key = "'user:' + #userId"
    )
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
                        .orElseThrow(
                                () -> new CartException(
                                        "Product is not present in cart"
                                )
                        );

        cart.removeItem(item);

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return buildCartResponse(cart);
    }


    /*
     * ============================================================
     * CLEAR CART
     * ============================================================
     *
     * The Redis cart is removed completely.
     *
     * Next GET /cart:
     *     Redis MISS
     *     -> MySQL
     *     -> empty cart response cached again
     */
    @Transactional
    @CacheEvict(
            value = "carts",
            key = "'user:' + #userId"
    )
    public void clearCart(Long userId) {

        validateUserId(userId);

        Cart cart = getCartEntity(userId);

        cart.getItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        log.info(
                "Cart cleared and Redis cache evicted. userId={}",
                userId
        );
    }


    /*
     * ============================================================
     * CHECKOUT
     * ============================================================
     *
     * Redis is evicted only when this method completes
     * successfully.
     *
     * If Order Service fails:
     *     fallback is triggered
     *     exception is thrown
     *     Redis cart remains intact
     *
     * This is important because the customer should not lose
     * their cart when checkout fails.
     */
    @Transactional
    @CacheEvict(
            value = "carts",
            key = "'user:' + #userId"
    )
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
                        .map(
                                item ->
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
                orderCheckoutService.placeOrder(request);

        if (response == null ||
                response.isEmpty()) {

            throw new CartException(
                    "Unable to create order"
            );
        }

        /*
         * Clear the database cart only after
         * Order Service successfully accepts the order.
         */
        cart.getItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        log.info(
                "Checkout successful. Cart cleared and Redis cache evicted. userId={}",
                userId
        );

        return response;
    }


    /*
     * ============================================================
     * GET OR CREATE CART
     * ============================================================
     */
    private Cart getOrCreateCart(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseGet(
                        () -> createEmptyCart(userId)
                );
    }


    /*
     * ============================================================
     * CREATE EMPTY CART
     * ============================================================
     */
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


    /*
     * ============================================================
     * GET CART ENTITY
     * ============================================================
     */
    private Cart getCartEntity(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseThrow(
                        () ->
                                new CartNotFoundException(
                                        "Cart not found for user: "
                                                + userId
                                )
                );
    }


    /*
     * ============================================================
     * GET PRODUCT
     * ============================================================
     */
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

        } catch (FeignException.NotFound exception) {

            log.warn(
                    "Product not found. productId={}",
                    productId
            );

            throw new CartNotFoundException(
                    "Product not found with ID: "
                            + productId
            );

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


    /*
     * ============================================================
     * VALIDATE PRODUCT
     * ============================================================
     */
    private void validateProduct(
            ProductResponse product) {

        if (!Boolean.TRUE.equals(
                product.getActive())) {

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


    /*
     * ============================================================
     * BUILD CART RESPONSE
     * ============================================================
     *
     * This DTO is what gets stored in Redis.
     *
     * We deliberately do NOT cache the JPA Cart entity.
     */
    private CartResponse buildCartResponse(
            Cart cart) {

        List<CartItemResponse> items =
                new ArrayList<>();

        double totalAmount = 0.0;

        int totalItems = 0;

        for (CartItem cartItem :
                cart.getItems()) {

            ProductResponse product =
                    getProduct(
                            cartItem.getProductId()
                    );

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

            totalItems +=
                    cartItem.getQuantity();
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }


    /*
     * ============================================================
     * VALIDATE USER
     * ============================================================
     */
    private void validateUserId(Long userId) {

        if (userId == null) {

            throw new CartException(
                    "Authenticated user ID is missing"
            );
        }
    }
}