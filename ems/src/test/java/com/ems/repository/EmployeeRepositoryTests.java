package com.ems.repository;

import com.ems.domain.Department;
import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import com.ems.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository slice tests against in-memory H2 (schema generated from entities). */
@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTests {

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private DepartmentRepository departments;

    @Autowired
    private UserRepository users;

    @Test
    void findsEmployeeByLinkedUser() {
        Department department = saveDepartment("Engineering", "ENG");
        User user = saveUser("jane.doe");
        Employee employee = saveEmployee("EMP-0101", "jane.doe@ems.local", department);
        employee.setUser(user);
        employees.saveAndFlush(employee);

        Optional<Employee> found = employees.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmployeeCode()).isEqualTo("EMP-0101");
    }

    @Test
    void countsByDepartment() {
        Department department = saveDepartment("Marketing", "MKT");
        saveEmployee("EMP-0201", "a@ems.local", department);
        saveEmployee("EMP-0202", "b@ems.local", department);

        assertThat(employees.countByDepartmentId(department.getId())).isEqualTo(2);
    }

    @Test
    void findsTopByEmployeeCodeDescending() {
        Department department = saveDepartment("Finance", "FIN");
        saveEmployee("EMP-0301", "c@ems.local", department);
        saveEmployee("EMP-0302", "d@ems.local", department);
        saveEmployee("EMP-0300", "e@ems.local", department);

        Optional<Employee> top = employees.findTopByOrderByEmployeeCodeDesc();

        assertThat(top).isPresent();
        assertThat(top.get().getEmployeeCode()).isEqualTo("EMP-0302");
    }

    @Test
    void listsUnlinkedEmployeesOnly() {
        Department department = saveDepartment("HR", "HRD");
        User user = saveUser("linked.user");
        Employee linked = saveEmployee("EMP-0401", "f@ems.local", department);
        linked.setUser(user);
        employees.saveAndFlush(linked);
        saveEmployee("EMP-0402", "g@ems.local", department);

        assertThat(employees.findByUserIsNullOrderByLastNameAscFirstNameAsc())
                .hasSize(1)
                .allSatisfy(e -> assertThat(e.getUser()).isNull());
    }

    // ---- fixtures -------------------------------------------------------------

    private Department saveDepartment(String name, String code) {
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        return departments.save(department);
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ems.local");
        user.setPassword("$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdu");
        user.setEnabled(true);
        return users.save(user);
    }

    private Employee saveEmployee(String code, String email, Department department) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFirstName("Test");
        employee.setLastName("Employee-" + code);
        employee.setEmail(email);
        employee.setJobTitle("Engineer");
        employee.setSalary(new BigDecimal("100000.00"));
        employee.setHireDate(LocalDate.of(2025, 1, 1));
        employee.setStatus(EmploymentStatus.ACTIVE);
        employee.setDepartment(department);
        return employees.save(employee);
    }
}
