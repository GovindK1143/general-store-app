
package com.cartservice.listener;

import com.cartservice.dto.CartPaymentStatusMessage;
import com.cartservice.service.CartCheckoutSnapshotService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CartPaymentStatusListener {

    private final CartCheckoutSnapshotService
            cartCheckoutSnapshotService;

    @KafkaListener(
            topics = "${payment.status.topic}",
            groupId = "cart-payment-status-group",
            containerFactory =
                    "cartPaymentStatusKafkaListenerContainerFactory"
    )
    public void onPaymentStatus(
            CartPaymentStatusMessage message) {

        if (message == null) {
            log.warn("Received an empty payment-status message");
            return;
        }

        String paymentStatus = message.getPaymentStatus();

        if (!"SUCCESS".equalsIgnoreCase(
                paymentStatus == null ? "" : paymentStatus.trim())) {

            log.info(
                    "Ignoring payment event because payment is not successful. " +
                            "orderId={}, paymentStatus={}",
                    message.getOrderId(),
                    paymentStatus
            );

            return;
        }

        if (message.getOrderId() == null
                || message.getUserId() == null) {

            log.error(
                    "Cannot process payment SUCCESS event: " +
                            "orderId or userId is missing"
            );

            return;
        }

        log.info(
                "Received successful payment event. orderId={}, userId={}",
                message.getOrderId(),
                message.getUserId()
        );

        // If checkout has already recorded its snapshot, this triggers
        // cleanup immediately. If the event arrives first, the service
        // records success and waits for the checkout snapshot.
        cartCheckoutSnapshotService.handlePaymentSuccess(
                message.getOrderId(),
                message.getUserId()
        );

        log.info(
                "Payment-success event processed. orderId={}, userId={}",
                message.getOrderId(),
                message.getUserId()
        );
    }
}
