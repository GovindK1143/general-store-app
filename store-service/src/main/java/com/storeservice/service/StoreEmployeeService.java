
package com.storeservice.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.storeservice.client.AuthUserClient;
import com.storeservice.dto.AuthUserResponse;
import com.storeservice.dto.StoreEmployeeRequest;
import com.storeservice.dto.StoreEmployeeResponse;
import com.storeservice.exception.EmployeeAssignmentConflictException;
import com.storeservice.exception.StoreEmployeeNotFoundException;
import com.storeservice.exception.StoreNotFoundException;
import com.storeservice.model.EmployeeDesignation;
import com.storeservice.model.EmploymentStatus;
import com.storeservice.model.Store;
import com.storeservice.model.StoreEmployee;
import com.storeservice.repository.StoreEmployeeRepository;
import com.storeservice.repository.StoreRepository;
import com.storeservice.dto.StoreEmployeeUpdateRequest;
import com.storeservice.dto.StoreEmployeeStatusRequest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreEmployeeService {

    private final StoreRepository storeRepository;

    private final StoreEmployeeRepository employeeRepository;

    private final AuthUserClient authUserClient;

    public StoreEmployeeResponse assignEmployee(
            Long storeId,
            StoreEmployeeRequest request,
            String authorization) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() ->
                        new StoreNotFoundException(
                                "Store not found with ID: " + storeId
                        )
                );

        Long userId = request.getUserId();

        if (employeeRepository.existsByUserId(userId)) {

            throw new EmployeeAssignmentConflictException(
                    "This user is already assigned to a store"
            );
        }

        String employeeCode = request.getEmployeeCode()
                .trim()
                .toUpperCase();

        if (employeeRepository.existsByEmployeeCodeIgnoreCase(
                employeeCode)) {

            throw new EmployeeAssignmentConflictException(
                    "Employee code already exists: " + employeeCode
            );
        }

        AuthUserResponse authUser = verifyAuthUser(
                userId,
                authorization
        );

        validateRoleAndDesignation(
                authUser.role(),
                request.getDesignation()
        );

        StoreEmployee employee = new StoreEmployee();

        employee.setUserId(authUser.userId());
        employee.setStore(store);
        employee.setEmployeeCode(employeeCode);
        employee.setDesignation(request.getDesignation());

        employee.setEmploymentStatus(
                EmploymentStatus.ACTIVE
        );

        LocalDateTime joinedAt = request.getJoinedAt();

        if (joinedAt == null) {
            joinedAt = LocalDateTime.now(
                    ZoneId.of(store.getTimezone())
            );
        }

        employee.setJoinedAt(joinedAt);

        return toResponse(
                employeeRepository.save(employee)
        );
    }

    public StoreEmployeeResponse updateEmployee(
            Long employeeId,
            StoreEmployeeUpdateRequest request,
            String authorization) {

        StoreEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new StoreEmployeeNotFoundException(
                                "Employee assignment not found: " + employeeId
                        )
                );

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() ->
                        new StoreNotFoundException(
                                "Store not found with ID: " + request.getStoreId()
                        )
                );

        String employeeCode = request.getEmployeeCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (employeeRepository.existsByEmployeeCodeIgnoreCaseAndIdNot(
                employeeCode,
                employeeId)) {

            throw new EmployeeAssignmentConflictException(
                    "Employee code already exists: " + employeeCode
            );
        }

        // Verify the existing Auth Service account and its current role.
        AuthUserResponse authUser = verifyAuthUser(
                employee.getUserId(),
                authorization
        );

        // Ensure the requested designation matches the Auth Service role.
        validateRoleAndDesignation(
                authUser.role(),
                request.getDesignation()
        );

        // Update the existing assignment. Do not create another row.
        employee.setStore(store);
        employee.setEmployeeCode(employeeCode);
        employee.setDesignation(request.getDesignation());

        return toResponse(employeeRepository.save(employee));
    }

    public StoreEmployeeResponse updateEmploymentStatus(
            Long employeeId,
            StoreEmployeeStatusRequest request) {

        StoreEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new StoreEmployeeNotFoundException(
                                "Employee assignment not found: " + employeeId
                        )
                );

        employee.setEmploymentStatus(request.getStatus());

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<StoreEmployeeResponse> getEmployeesByStore(
            Long storeId) {

        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(
                    "Store not found with ID: " + storeId
            );
        }

        return employeeRepository
                .findByStore_IdOrderByCreatedAtDesc(storeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreEmployeeResponse getEmployeeById(
            Long employeeId) {

        StoreEmployee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new StoreEmployeeNotFoundException(
                                "Employee assignment not found: " + employeeId
                        )
                );

        return toResponse(employee);
    }

    private AuthUserResponse verifyAuthUser(
            Long userId,
            String authorization) {

        try {

            AuthUserResponse user =
                    authUserClient.getUserByIdForAdmin(
                            userId,
                            authorization
                    );

            if (user == null || user.userId() == null ||
                    !userId.equals(user.userId()) ||
                    user.role() == null) {

                throw new IllegalArgumentException(
                        "Auth Service returned an invalid user response"
                );
            }

            return user;

        } catch (FeignException.NotFound exception) {

            throw new StoreEmployeeNotFoundException(
                    "Auth Service user account not found: " + userId
            );

        } catch (FeignException.Unauthorized exception) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Auth Service rejected the authentication token"
            );

        } catch (FeignException.Forbidden exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "ADMIN authorization is required to verify employee accounts"
            );

        } catch (FeignException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify employee account with Auth Service"
            );
        }
    }

    private void validateRoleAndDesignation(
            String role,
            EmployeeDesignation designation) {

        if (role == null ||
                designation == null) {

            throw new IllegalArgumentException(
                    "Employee role and designation are required"
            );
        }

        boolean valid = switch (role) {

            case "STORE_MANAGER" ->
                    designation == EmployeeDesignation.STORE_MANAGER;

            case "STORE_EMPLOYEE" ->
                    designation != EmployeeDesignation.STORE_MANAGER;

            default -> false;
        };

        if (!valid) {

            throw new IllegalArgumentException(
                    "Auth role " + role +
                            " is not compatible with designation " + designation
            );
        }
    }

    private StoreEmployeeResponse toResponse(
            StoreEmployee employee) {

        Store store = employee.getStore();

        return new StoreEmployeeResponse(
                employee.getId(),
                employee.getUserId(),
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                employee.getEmployeeCode(),
                employee.getDesignation(),
                employee.getEmploymentStatus(),
                employee.getJoinedAt(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
