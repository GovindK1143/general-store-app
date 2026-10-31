
package com.storeservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "stores",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stores_store_code",
                        columnNames = "store_code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_code", nullable = false, length = 30)
    private String storeCode;

    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName;

    @Column(name = "address_line1", nullable = false, length = 250)
    private String addressLine1;

    @Column(name = "address_line2", length = 250)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(
            name = "max_delivery_radius_km",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal maxDeliveryRadiusKm =
            new BigDecimal("5.00");

    @Column(name = "online_fulfillment_enabled", nullable = false)
    private Boolean onlineFulfillmentEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreStatus status = StoreStatus.INACTIVE;

    @Column(name = "timezone", nullable = false, length = 60)
    private String timezone = "Asia/Kolkata";

    @Column(name = "gstin", length = 20)
    private String gstin;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
