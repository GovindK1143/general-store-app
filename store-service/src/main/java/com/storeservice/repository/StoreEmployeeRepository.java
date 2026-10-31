
package com.storeservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.storeservice.model.StoreEmployee;
import com.storeservice.model.EmploymentStatus;

@Repository
public interface StoreEmployeeRepository
        extends JpaRepository<StoreEmployee, Long> {

    Optional<StoreEmployee> findByUserId(Long userId);

    Optional<StoreEmployee> findByEmployeeCodeIgnoreCase(
            String employeeCode
    );

    List<StoreEmployee> findByStore_IdOrderByCreatedAtDesc(
            Long storeId
    );

    List<StoreEmployee> findByStore_IdAndEmploymentStatus(
            Long storeId,
            EmploymentStatus employmentStatus
    );

    boolean existsByUserId(Long userId);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(
            String employeeCode,
            Long employeeId
    );
}
