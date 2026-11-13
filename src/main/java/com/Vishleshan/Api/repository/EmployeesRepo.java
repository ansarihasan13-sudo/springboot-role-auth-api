
package com.Vishleshan.Api.repository;

import com.Vishleshan.Api.entity.Employees;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeesRepo
        extends JpaRepository<Employees, UUID> {

    Optional<Employees> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByIsActiveTrue();

    long countByIsActiveFalse();

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndIsActiveTrue();

    long countByDeletedAtIsNullAndIsActiveFalse();

    List<Employees> findAllByDeletedAtIsNull();

    List<Employees> findAllByDeletedAtIsNotNull();

    Optional<Employees> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Employees> findByIdAndDeletedAtIsNotNull(UUID id);

}
