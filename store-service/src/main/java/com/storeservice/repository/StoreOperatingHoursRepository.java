
package com.storeservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.storeservice.model.StoreOperatingHours;

@Repository
public interface StoreOperatingHoursRepository
        extends JpaRepository<StoreOperatingHours, Long> {

    List<StoreOperatingHours> findByStore_IdOrderByDayOfWeekAsc(
            Long storeId
    );

    Optional<StoreOperatingHours> findByStore_IdAndDayOfWeek(
            Long storeId,
            Byte dayOfWeek
    );

    boolean existsByStore_IdAndDayOfWeek(
            Long storeId,
            Byte dayOfWeek
    );
}
