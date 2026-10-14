package com.orderservice.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderservice.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

	Optional<Order> findByIdAndUserId(Long id, Long userId);

	List<Order> findByOrderStatusAndPaymentStatusAndLastPaymentAttemptAtBeforeOrderByOrderDateAsc(
			String orderStatus,
			String paymentStatus,
			LocalDateTime lastPaymentAttemptAt,
			Pageable pageable
	);

	// Lock an order belonging to a specific customer during payment retry.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
       SELECT o
       FROM Order o
       WHERE o.id = :orderId
         AND o.userId = :userId
       """)
	Optional<Order> findByIdAndUserIdForUpdate(
			@Param("orderId") Long orderId,
			@Param("userId") Long userId
	);


	// Lock an order while processing a payment-status event.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
       SELECT o
       FROM Order o
       WHERE o.id = :orderId
       """)
	Optional<Order> findByIdForUpdate(
			@Param("orderId") Long orderId
	);
}