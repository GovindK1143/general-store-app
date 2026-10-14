
package com.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.orderservice.model.Order;
import com.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(
        prefix = "quickmart.payment-recovery",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentRecoveryScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${quickmart.payment-recovery.retry-interval-seconds:30}")
    private long retryIntervalSeconds;

    @Value("${quickmart.payment-recovery.batch-size:10}")
    private int batchSize;

    @Scheduled(
            fixedDelayString =
                    "${quickmart.payment-recovery.fixed-delay-ms:15000}",
            initialDelayString =
                    "${quickmart.payment-recovery.initial-delay-ms:15000}"
    )
    public void recoverPendingPayments() {

        if (retryIntervalSeconds < 1) {
            log.error(
                    "Payment recovery skipped: retry interval must be positive"
            );
            return;
        }

        LocalDateTime cutoff =
                LocalDateTime.now().minusSeconds(retryIntervalSeconds);

        int boundedBatchSize =
                Math.max(1, Math.min(batchSize, 100));

        List<Order> dueOrders;

        try {
            dueOrders = orderRepository
                    .findByOrderStatusAndPaymentStatusAndLastPaymentAttemptAtBeforeOrderByOrderDateAsc(
                            "PENDING",
                            "PENDING",
                            cutoff,
                            PageRequest.of(0, boundedBatchSize)
                    );

        } catch (Exception exception) {
            log.error(
                    "Unable to query pending orders for payment recovery",
                    exception
            );
            return;
        }

        if (dueOrders.isEmpty()) {
            log.debug("No pending payments are due for automatic recovery");
            return;
        }

        log.info(
                "Found {} pending orders eligible for payment recovery",
                dueOrders.size()
        );

        for (Order candidate : dueOrders) {

            Long orderId = candidate.getId();

            if (orderId == null) {
                continue;
            }

            try {
                boolean attempted =
                        orderService.retryPendingPaymentIfDue(
                                orderId,
                                cutoff
                        );

                if (attempted) {
                    log.info(
                            "Automatic payment recovery attempt finished. " +
                                    "orderId={}",
                            orderId
                    );
                }

            } catch (Exception exception) {
                // Isolate failures so one order does not stop the batch.
                log.error(
                        "Automatic payment recovery failed for orderId={}",
                        orderId,
                        exception
                );
            }
        }
    }
}
