
package com.inventoryservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inventoryservice.model.InventoryReservation;
import com.inventoryservice.model.enums.BusinessReferenceType;
import com.inventoryservice.model.enums.ReservationStatus;

@Repository
public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation>
    findByBusinessReferenceTypeAndBusinessReferenceId(
            BusinessReferenceType businessReferenceType,
            Long businessReferenceId
    );

    Optional<InventoryReservation> findByIdempotencyKey(
            String idempotencyKey
    );

    boolean existsByBusinessReferenceTypeAndBusinessReferenceId(
            BusinessReferenceType businessReferenceType,
            Long businessReferenceId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT reservation
            FROM InventoryReservation reservation
            WHERE reservation.id = :reservationId
            """)
    Optional<InventoryReservation> findByIdForUpdate(
            @Param("reservationId") Long reservationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT reservation
            FROM InventoryReservation reservation
            WHERE reservation.businessReferenceType = :referenceType
              AND reservation.businessReferenceId = :referenceId
            """)
    Optional<InventoryReservation> findByBusinessReferenceForUpdate(
            @Param("referenceType") BusinessReferenceType referenceType,
            @Param("referenceId") Long referenceId
    );

    List<InventoryReservation>
    findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            ReservationStatus status,
            LocalDateTime currentTime
    );
}
