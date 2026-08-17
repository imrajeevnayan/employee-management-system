package com.ems.service;

import com.ems.domain.Department;
import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import com.ems.domain.Role;
import com.ems.domain.RoleType;
import com.ems.domain.User;
import com.ems.dto.EmployeeForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests for EmployeeService business rules (no Spring context). */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createRejectsDuplicateEmail() {
        when(employeeRepository.existsByEmailIgnoreCase("rahul.verma@ems.local")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(validForm()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void createGeneratesSequentialEmployeeCode() {
        when(employeeRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering()));

        Employee latest = new Employee();
        latest.setEmployeeCode("EMP-0007");
        when(employeeRepository.findTopByOrderByEmployeeCodeDesc()).thenReturn(Optional.of(latest));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = employeeService.create(validForm());

        assertThat(created.getEmployeeCode()).isEqualTo("EMP-0008");
    }

    @Test
    void managerCanEditEmployeeFromOwnDepartment() {
        User manager = managerIn(engineering());
        Employee target = employeeIn(engineering());

        assertThatCode(() -> employeeService.assertCanManage(target, manager))
                .doesNotThrowAnyException();
    }

    @Test
    void managerCannotEditEmployeeFromAnotherDepartment() {
        User manager = managerIn(engineering());
        Employee target = employeeIn(finance());

        assertThatThrownBy(() -> employeeService.assertCanManage(target, manager))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void employeeCanOnlyViewOwnRecord() {
        User employee = userWithRole(RoleType.EMPLOYEE);
        Employee self = employeeIn(engineering());
        self.setId(3L);
        employee.setEmployee(self);

        Employee other = employeeIn(engineering());
        other.setId(9L);

        assertThatCode(() -> employeeService.assertCanView(self, employee)).doesNotThrowAnyException();
        assertThatThrownBy(() -> employeeService.assertCanView(other, employee))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // ---- fixtures -------------------------------------------------------------

    private EmployeeForm validForm() {
        EmployeeForm form = new EmployeeForm();
        form.setFirstName("Rahul");
        form.setLastName("Verma");
        form.setEmail("rahul.verma@ems.local");
        form.setPhone("+91 99001 10004");
        form.setJobTitle("Backend Engineer");
        form.setSalary(new BigDecimal("1250000.00"));
        form.setHireDate(LocalDate.of(2024, 2, 19));
        form.setStatus(EmploymentStatus.ACTIVE);
        form.setDepartmentId(1L);
        return form;
    }

    private Department engineering() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        department.setCode("ENG");
        return department;
    }

    private Department finance() {
        Department department = new Department();
        department.setId(4L);
        department.setName("Finance");
        department.setCode("FIN");
        return department;
    }

    private Employee employeeIn(Department department) {
        Employee employee = new Employee();
        employee.setFirstName("Test");
        employee.setLastName("Employee");
        employee.setDepartment(department);
        return employee;
    }

    private User userWithRole(RoleType type) {
        Role role = new Role();
        role.setName(type);
        User user = new User();
        user.setUsername("test.user");
        user.getRoles().add(role);
        return user;
    }

    private User managerIn(Department department) {
        User manager = userWithRole(RoleType.MANAGER);
        Employee self = employeeIn(department);
        self.setId(1L);
        manager.setEmployee(self);
        return manager;
    }
}
