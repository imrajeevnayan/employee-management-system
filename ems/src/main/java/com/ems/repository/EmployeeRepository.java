package com.ems.repository;

import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByUserId(Long userId);

    List<Employee> findByDepartmentIdOrderByHireDateDesc(Long departmentId);

    List<Employee> findByUserIsNullOrderByLastNameAscFirstNameAsc();

    List<Employee> findTop5ByOrderByHireDateDesc();

    Optional<Employee> findTopByOrderByEmployeeCodeDesc();

    long countByDepartmentId(Long departmentId);

    long countByStatus(EmploymentStatus status);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
