
package com.cartservice.repository;

import java.util.List;

import com.cartservice.model.CartCheckoutSnapshotItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartCheckoutSnapshotItemRepository
        extends JpaRepository<CartCheckoutSnapshotItem, Long> {

    List<CartCheckoutSnapshotItem>
    findByOrderIdOrderByProductIdAsc(Long orderId);
}
