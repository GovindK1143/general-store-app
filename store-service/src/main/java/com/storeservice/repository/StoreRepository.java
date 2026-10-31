
package com.storeservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.storeservice.model.Store;
import com.storeservice.model.StoreStatus;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreCodeIgnoreCase(String storeCode);

    boolean existsByStoreCodeIgnoreCase(String storeCode);

    List<Store> findByStatusOrderByStoreNameAsc(StoreStatus status);

    List<Store> findAllByOrderByCreatedAtDesc();
}
