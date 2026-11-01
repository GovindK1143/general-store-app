
package com.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventoryservice.model.ProcessedEvent;

@Repository
public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndConsumerName(
            String eventId,
            String consumerName
    );
}
