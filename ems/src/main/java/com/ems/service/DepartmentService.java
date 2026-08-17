package com.ems.service;

import com.ems.domain.Department;
import com.ems.domain.Employee;
import com.ems.dto.DepartmentForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    public List<Employee> employeesOf(Long departmentId) {
        return employeeRepository.findByDepartmentIdOrderByHireDateDesc(departmentId);
    }

    public List<DepartmentSummary> findAllWithCounts() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(department -> new DepartmentSummary(
                        department, employeeRepository.countByDepartmentId(department.getId())))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Department create(DepartmentForm form) {
        String name = form.getName().trim();
        String code = form.getCode().trim().toUpperCase();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A department named '" + name + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Department code '" + code + "' is already in use");
        }
        Department department = new Department();
        applyForm(department, form, name, code);
        return departmentRepository.save(department);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Department update(Long id, DepartmentForm form) {
        Department department = findById(id);
        String name = form.getName().trim();
        String code = form.getCode().trim().toUpperCase();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A department named '" + name + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Department code '" + code + "' is already in use");
        }
        applyForm(department, form, name, code);
        return departmentRepository.save(department);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void delete(Long id) {
        Department department = findById(id);
        long assigned = employeeRepository.countByDepartmentId(id);
        if (assigned > 0) {
            throw new IllegalStateException("Cannot delete '" + department.getName() + "': "
                    + assigned + " employee(s) are still assigned to it.");
        }
        departmentRepository.delete(department);
    }

    private void applyForm(Department department, DepartmentForm form, String name, String code) {
        department.setName(name);
        department.setCode(code);
        department.setLocation(form.getLocation() == null || form.getLocation().isBlank()
                ? null : form.getLocation().trim());
        department.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
    }

    public record DepartmentSummary(Department department, long employeeCount) {
    }
}
