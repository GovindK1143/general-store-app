package com.userservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.userservice.model.CustomerAddress;

@Repository
public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress>
    findAllByUser_IdOrderByDefaultAddressDescIdDesc(Long userId);

    Optional<CustomerAddress> findByIdAndUser_Id(
            Long id,
            Long userId
    );

    boolean existsByUser_Id(Long userId);
}