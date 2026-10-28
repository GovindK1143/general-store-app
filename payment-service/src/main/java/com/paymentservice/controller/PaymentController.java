package com.paymentservice.controller;

import com.paymentservice.dto.PaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.paymentservice.dto.AdminPaymentSummary;
import com.paymentservice.model.Payment;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process a payment for an order.
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        Long authenticatedUserId =
                (Long) httpRequest.getAttribute("userId");

        String role =
                (String) httpRequest.getAttribute("role");

        return ResponseEntity.ok(
                paymentService.processPayment(
                        request,
                        authenticatedUserId,
                        role
                )
        );
    }

    /**
     * Get payment details using order ID.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        Long authenticatedUserId =
                (Long) httpRequest.getAttribute("userId");

        String role =
                (String) httpRequest.getAttribute("role");

        return ResponseEntity.ok(
                paymentService.getPaymentByOrderId(
                        orderId,
                        authenticatedUserId,
                        role
                )
        );
    }

    /**
     * Get payment status using order ID.
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        Long authenticatedUserId =
                (Long) httpRequest.getAttribute("userId");

        String role =
                (String) httpRequest.getAttribute("role");

        return ResponseEntity.ok(
                paymentService.getPaymentStatus(
                        orderId,
                        authenticatedUserId,
                        role
                )
        );
    }

    // =========================================================
    // ADMIN: GET ALL PAYMENTS
    // GET /payments/admin/all
    // =========================================================

    @GetMapping("/admin/all")
    public ResponseEntity<List<Payment>> getAllPaymentsForAdmin() {
        return ResponseEntity.ok(
                paymentService.getAllPaymentsForAdmin()
        );
    }

    // =========================================================
    // ADMIN: PAYMENT SUMMARY
    // GET /payments/admin/summary
    // =========================================================

    @GetMapping("/admin/summary")
    public ResponseEntity<AdminPaymentSummary> getAdminPaymentSummary() {
        return ResponseEntity.ok(
                paymentService.getAdminPaymentSummary()
        );
    }
}