
package com.cartservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartservice.exception.CartException;
import com.cartservice.model.Cart;
import com.cartservice.model.CartCheckoutSnapshot;
import com.cartservice.model.CartCheckoutSnapshotItem;
import com.cartservice.model.CartItem;
import com.cartservice.repository.CartCheckoutSnapshotItemRepository;
import com.cartservice.repository.CartCheckoutSnapshotRepository;
import com.cartservice.repository.CartItemRepository;
import com.cartservice.repository.CartRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartCheckoutSnapshotService {

    private final CartCheckoutSnapshotRepository snapshotRepository;
    private final CartCheckoutSnapshotItemRepository snapshotItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    /**
     * Records the original cart items associated with an order.
     *
     * This method also handles immediate payment success.
     * If a Kafka SUCCESS event arrived before this snapshot was ready,
     * the previously recorded payment success is processed here.
     */
    @Transactional
    @CacheEvict(value = "carts", key = "'user:' + #userId")
    public void recordCheckoutSnapshot(
            Long orderId,
            Long userId,
            List<CartCheckoutSnapshotItem> capturedItems,
            boolean paymentAlreadySucceeded) {

        validateIdentifiers(orderId, userId);

        if (capturedItems == null || capturedItems.isEmpty()) {
            throw new CartException(
                    "Checkout snapshot cannot be empty"
            );
        }

        // Create a placeholder only if this order has no snapshot yet.
        // INSERT IGNORE preserves any SUCCESS event already recorded.
        snapshotRepository.claimSnapshot(
                orderId,
                userId,
                false,
                LocalDateTime.now()
        );

        CartCheckoutSnapshot snapshot =
                getSnapshotForUpdate(orderId);

        validateSnapshotOwner(snapshot, userId);

        if (!Boolean.TRUE.equals(snapshot.getSnapshotReady())) {

            List<CartCheckoutSnapshotItem> snapshotItems =
                    new ArrayList<>();

            for (CartCheckoutSnapshotItem captured : capturedItems) {

                if (captured.getCartItemId() == null
                        || captured.getProductId() == null
                        || captured.getQuantity() == null
                        || captured.getQuantity() <= 0) {

                    throw new CartException(
                            "Invalid item in checkout snapshot"
                    );
                }

                CartCheckoutSnapshotItem item =
                        new CartCheckoutSnapshotItem();

                item.setOrderId(orderId);
                item.setCartItemId(captured.getCartItemId());
                item.setProductId(captured.getProductId());
                item.setQuantity(captured.getQuantity());

                snapshotItems.add(item);
            }

            snapshotItemRepository.saveAll(snapshotItems);

            snapshot.setSnapshotReady(true);
        }

        // Never change SUCCESS back to false.
        if (paymentAlreadySucceeded) {
            snapshot.setPaymentSucceeded(true);
        }

        snapshotRepository.save(snapshot);

        processIfPaymentSucceeded(snapshot);
    }

    /**
     * Called by the Kafka listener when a payment SUCCESS event arrives.
     *
     * A success event may arrive before the checkout snapshot is saved.
     * In that case, record SUCCESS now and wait until the snapshot is ready.
     */
    @Transactional
    @CacheEvict(value = "carts", key = "'user:' + #userId")
    public void handlePaymentSuccess(Long orderId, Long userId) {

        validateIdentifiers(orderId, userId);

        snapshotRepository.claimSnapshot(
                orderId,
                userId,
                true,
                LocalDateTime.now()
        );

        CartCheckoutSnapshot snapshot =
                getSnapshotForUpdate(orderId);

        validateSnapshotOwner(snapshot, userId);

        snapshot.setPaymentSucceeded(true);

        snapshotRepository.save(snapshot);

        processIfPaymentSucceeded(snapshot);
    }

    private void processIfPaymentSucceeded(
            CartCheckoutSnapshot snapshot) {

        if (!Boolean.TRUE.equals(snapshot.getPaymentSucceeded())) {
            return;
        }

        if (!Boolean.TRUE.equals(snapshot.getSnapshotReady())) {
            log.info(
                    "Payment succeeded; waiting for checkout snapshot. orderId={}",
                    snapshot.getOrderId()
            );
            return;
        }

        if (Boolean.TRUE.equals(snapshot.getProcessed())) {
            log.info(
                    "Cart cleanup already processed. orderId={}",
                    snapshot.getOrderId()
            );
            return;
        }

        Long orderId = snapshot.getOrderId();
        Long userId = snapshot.getUserId();

        Cart cart = cartRepository
                .findByUserIdForUpdate(userId)
                .orElse(null);

        if (cart == null) {

            // There is no cart left to update.
            markProcessed(snapshot);

            log.info(
                    "No cart found; checkout cleanup completed. orderId={}, userId={}",
                    orderId,
                    userId
            );

            return;
        }

        List<CartCheckoutSnapshotItem> purchasedItems =
                snapshotItemRepository
                        .findByOrderIdOrderByProductIdAsc(orderId);

        for (CartCheckoutSnapshotItem purchased : purchasedItems) {

            // Match the original row, not just the product ID.
            // A removed and re-added product will have a different row ID.
            CartItem currentItem = cartItemRepository
                    .findByIdAndCart_Id(
                            purchased.getCartItemId(),
                            cart.getId()
                    )
                    .orElse(null);

            if (currentItem == null) {
                continue;
            }

            if (!Objects.equals(
                    currentItem.getProductId(),
                    purchased.getProductId())) {
                continue;
            }

            int currentQuantity = currentItem.getQuantity();
            int purchasedQuantity = purchased.getQuantity();

            if (currentQuantity > purchasedQuantity) {

                // Preserve quantities added after checkout.
                currentItem.setQuantity(
                        currentQuantity - purchasedQuantity
                );

            } else if (currentQuantity == purchasedQuantity) {

                // The original purchased quantity is still present.
                cart.removeItem(currentItem);

            } else {

                // A smaller quantity may reflect a later user edit.
                // Conservatively preserve it rather than deleting it.
                log.warn(
                        "Preserving cart item with reduced quantity. " +
                                "orderId={}, cartItemId={}, currentQuantity={}, " +
                                "purchasedQuantity={}",
                        orderId,
                        currentItem.getId(),
                        currentQuantity,
                        purchasedQuantity
                );
            }
        }

        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        markProcessed(snapshot);

        log.info(
                "Cart cleanup completed after payment success. " +
                        "orderId={}, userId={}, cartId={}",
                orderId,
                userId,
                cart.getId()
        );
    }

    private CartCheckoutSnapshot getSnapshotForUpdate(Long orderId) {

        return snapshotRepository
                .findByOrderIdForUpdate(orderId)
                .orElseThrow(
                        () -> new CartException(
                                "Checkout snapshot not found for order: "
                                        + orderId
                        )
                );
    }

    private void validateIdentifiers(Long orderId, Long userId) {

        if (orderId == null || userId == null) {
            throw new CartException(
                    "Order ID and user ID are required for cart cleanup"
            );
        }
    }

    private void validateSnapshotOwner(
            CartCheckoutSnapshot snapshot,
            Long userId) {

        if (!Objects.equals(snapshot.getUserId(), userId)) {
            throw new CartException(
                    "Checkout snapshot user does not match the payment event"
            );
        }
    }

    private void markProcessed(CartCheckoutSnapshot snapshot) {

        snapshot.setProcessed(true);
        snapshot.setProcessedAt(LocalDateTime.now());

        snapshotRepository.save(snapshot);
    }
}
