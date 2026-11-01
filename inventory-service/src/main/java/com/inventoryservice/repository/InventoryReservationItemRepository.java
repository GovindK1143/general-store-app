
package com.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventoryservice.model.InventoryReservationItem;

@Repository
public interface InventoryReservationItemRepository
        extends JpaRepository<InventoryReservationItem, Long> {

    List<InventoryReservationItem>
    findByReservation_IdOrderByProductIdAsc(Long reservationId);

    Optional<InventoryReservationItem>
    findByReservation_IdAndProductId(
            Long reservationId,
            Long productId
    );

    boolean existsByReservation_IdAndProductId(
            Long reservationId,
            Long productId
    );
}
