
package com.storeservice.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "store_employees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_employees_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_store_employees_code",
                        columnNames = "employee_code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StoreEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Logical reference to users_db.users.id.
    // No cross-database JPA relationship.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_store_employees_store"
            )
    )
    private Store store;

    @Column(
            name = "employee_code",
            nullable = false,
            length = 40
    )
    private String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "designation", nullable = false, length = 40)
    private EmployeeDesignation designation;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "employment_status",
            nullable = false,
            length = 20
    )
    private EmploymentStatus employmentStatus =
            EmploymentStatus.ACTIVE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

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
