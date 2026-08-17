package com.ems.service;

import com.ems.domain.Department;
import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import com.ems.domain.RoleType;
import com.ems.domain.User;
import com.ems.dto.EmployeeForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Full-text search across code/name/email/job title with optional
     * department and status filters, built as a dynamic Specification so no
     * filter means no predicate (no null-parameter SQL issues).
     */
    public Page<Employee> search(String q, Long departmentId, EmploymentStatus status, Pageable pageable) {
        List<Specification<Employee>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String term = "%" + q.trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("employeeCode")), term),
                    cb.like(cb.lower(root.<String>get("firstName")), term),
                    cb.like(cb.lower(root.<String>get("lastName")), term),
                    cb.like(cb.lower(root.<String>get("email")), term),
                    cb.like(cb.lower(root.<String>get("jobTitle")), term)));
        }
        if (departmentId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return employeeRepository.findAll(Specification.allOf(specs), pageable);
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    public List<Employee> unlinkedEmployees() {
        return employeeRepository.findByUserIsNullOrderByLastNameAscFirstNameAsc();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Employee create(EmployeeForm form) {
        String email = form.getEmail().trim();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An employee with email '" + email + "' already exists");
        }
        Department department = loadDepartment(form.getDepartmentId());
        Employee employee = new Employee();
        applyForm(employee, form, department);
        employee.setEmployeeCode(nextEmployeeCode());
        return employeeRepository.save(employee);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public Employee update(Long id, EmployeeForm form) {
        Employee employee = findById(id);
        String email = form.getEmail().trim();
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("An employee with email '" + email + "' already exists");
        }
        applyForm(employee, form, loadDepartment(form.getDepartmentId()));
        return employeeRepository.save(employee);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        employeeRepository.deleteById(id);
    }

    /** ADMIN/HR see everyone; MANAGER only their own department; EMPLOYEE only themselves. */
    public void assertCanView(Employee target, User current) {
        if (current.hasRole(RoleType.ADMIN) || current.hasRole(RoleType.HR) || current.hasRole(RoleType.MANAGER)) {
            return;
        }
        Employee self = current.getEmployee();
        if (self == null || !self.getId().equals(target.getId())) {
            throw new AccessDeniedException("You can only view your own employee record");
        }
    }

    /** ADMIN/HR manage everyone; MANAGER only employees of their own department. */
    public void assertCanManage(Employee target, User current) {
        if (current.hasRole(RoleType.ADMIN) || current.hasRole(RoleType.HR)) {
            return;
        }
        if (!current.hasRole(RoleType.MANAGER)) {
            throw new AccessDeniedException("You do not have permission to manage employee records");
        }
        Employee self = current.getEmployee();
        if (self == null
                || !self.getDepartment().getId().equals(target.getDepartment().getId())) {
            throw new AccessDeniedException("Managers can only edit employees in their own department");
        }
    }

    private void applyForm(Employee employee, EmployeeForm form, Department department) {
        employee.setFirstName(form.getFirstName().trim());
        employee.setLastName(form.getLastName().trim());
        employee.setEmail(form.getEmail().trim());
        employee.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        employee.setJobTitle(form.getJobTitle().trim());
        employee.setSalary(form.getSalary());
        employee.setHireDate(form.getHireDate());
        employee.setStatus(form.getStatus());
        employee.setDepartment(department);
    }

    private Department loadDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
    }

    private String nextEmployeeCode() {
        return employeeRepository.findTopByOrderByEmployeeCodeDesc()
                .map(latest -> "EMP-%04d".formatted(
                        Integer.parseInt(latest.getEmployeeCode().substring(4)) + 1))
                .orElse("EMP-0001");
    }
}
