
package com.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventoryservice.model.OutboxEvent;
import com.inventoryservice.model.enums.OutboxPublishStatus;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent>
    findTop100ByPublishStatusOrderByCreatedAtAsc(
            OutboxPublishStatus publishStatus
    );

    boolean existsByEventId(String eventId);
}
