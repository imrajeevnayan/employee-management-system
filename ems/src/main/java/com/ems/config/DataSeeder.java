package com.ems.config;

import com.ems.domain.Department;
import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import com.ems.domain.Role;
import com.ems.domain.RoleType;
import com.ems.domain.User;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.RoleRepository;
import com.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Idempotent first-boot seeding: roles, demo departments, employees and one
 * account per role so the RBAC matrix can be explored immediately.
 * Skipped entirely once any user exists. Passwords come from ems.seed.*
 * properties (overridable via EMS_*_PASSWORD environment variables).
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ems.seed.admin-password:Admin@123}")
    private String adminPassword;

    @Value("${ems.seed.hr-password:Hr@12345}")
    private String hrPassword;

    @Value("${ems.seed.manager-password:Manager@123}")
    private String managerPassword;

    @Value("${ems.seed.employee-password:Employee@123}")
    private String employeePassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users - skipping demo data seed");
            return;
        }

        Role adminRole = role(RoleType.ADMIN);
        Role hrRole = role(RoleType.HR);
        Role managerRole = role(RoleType.MANAGER);
        Role employeeRole = role(RoleType.EMPLOYEE);

        Department engineering = department("Engineering", "ENG", "Bengaluru",
                "Product engineering: backend, frontend, QA and platform teams.");
        Department humanResources = department("Human Resources", "HRD", "Bengaluru",
                "Recruiting, onboarding, payroll and employee relations.");
        Department marketing = department("Marketing", "MKT", "Mumbai",
                "Brand, content and demand generation.");
        Department finance = department("Finance", "FIN", "Delhi",
                "Accounting, budgeting and financial planning.");

        // Accounts: one per role; three are linked to employee records.
        User admin = user("admin", "admin@ems.local", adminPassword, Set.of(adminRole));
        User hrKavya = user("hr.kavya", "kavya.sharma@ems.local", hrPassword, Set.of(hrRole));
        User mgrArjun = user("mgr.arjun", "arjun.mehta@ems.local", managerPassword, Set.of(managerRole));
        User empPriya = user("emp.priya", "priya.nair@ems.local", employeePassword, Set.of(employeeRole));

        // Employees. Arjun (manager) and Vikram/Sneha/Rahul share Engineering,
        // so the "managers edit only their own department" rule can be observed.
        Employee arjun = employee("EMP-0001", "Arjun", "Mehta", "arjun.mehta@ems.local",
                "+91 99001 10001", "Senior Engineering Manager", new BigDecimal("3200000.00"),
                LocalDate.of(2019, 4, 15), EmploymentStatus.ACTIVE, engineering, mgrArjun);
        employee("EMP-0002", "Kavya", "Sharma", "kavya.sharma@ems.local",
                "+91 99001 10002", "HR Business Partner", new BigDecimal("1450000.00"),
                LocalDate.of(2021, 1, 11), EmploymentStatus.ACTIVE, humanResources, hrKavya);
        employee("EMP-0003", "Priya", "Nair", "priya.nair@ems.local",
                "+91 99001 10003", "Marketing Specialist", new BigDecimal("980000.00"),
                LocalDate.of(2023, 7, 3), EmploymentStatus.ACTIVE, marketing, empPriya);
        employee("EMP-0004", "Rahul", "Verma", "rahul.verma@ems.local",
                "+91 99001 10004", "Backend Engineer", new BigDecimal("1250000.00"),
                LocalDate.of(2024, 2, 19), EmploymentStatus.ACTIVE, engineering, null);
        employee("EMP-0005", "Sneha", "Iyer", "sneha.iyer@ems.local",
                "+91 99001 10005", "QA Engineer", new BigDecimal("1050000.00"),
                LocalDate.of(2022, 9, 1), EmploymentStatus.ON_LEAVE, engineering, null);
        employee("EMP-0006", "Amit", "Patil", "amit.patil@ems.local",
                "+91 99001 10006", "Financial Analyst", new BigDecimal("1150000.00"),
                LocalDate.of(2020, 11, 23), EmploymentStatus.ACTIVE, finance, null);
        employee("EMP-0007", "Divya", "Reddy", "divya.reddy@ems.local",
                "+91 99001 10007", "Content Lead", new BigDecimal("1320000.00"),
                LocalDate.of(2021, 6, 14), EmploymentStatus.ACTIVE, marketing, null);
        employee("EMP-0008", "Karan", "Malhotra", "karan.malhotra@ems.local",
                "+91 99001 10008", "Accounts Executive", new BigDecimal("900000.00"),
                LocalDate.of(2018, 3, 5), EmploymentStatus.TERMINATED, finance, null);
        employee("EMP-0009", "Neha", "Kulkarni", "neha.kulkarni@ems.local",
                "+91 99001 10009", "Recruiter", new BigDecimal("870000.00"),
                LocalDate.of(2024, 8, 12), EmploymentStatus.ACTIVE, humanResources, null);
        employee("EMP-0010", "Vikram", "Singh", "vikram.singh@ems.local",
                "+91 99001 10010", "DevOps Engineer", new BigDecimal("1580000.00"),
                LocalDate.of(2026, 1, 5), EmploymentStatus.ACTIVE, engineering, null);

        log.info("Seeded EMS demo data: 4 roles, 4 users, 4 departments, 10 employees "
                + "(manager demo user 'mgr.arjun' is linked to {} in Engineering)", arjun.getEmployeeCode());
    }

    private Role role(RoleType type) {
        return roleRepository.findByName(type).orElseGet(() -> {
            Role role = new Role();
            role.setName(type);
            return roleRepository.save(role);
        });
    }

    private Department department(String name, String code, String location, String description) {
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setLocation(location);
        department.setDescription(description);
        return departmentRepository.save(department);
    }

    private User user(String username, String email, String rawPassword, Set<Role> roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    private Employee employee(String code, String firstName, String lastName, String email,
                              String phone, String jobTitle, BigDecimal salary, LocalDate hireDate,
                              EmploymentStatus status, Department department, User linkedUser) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setJobTitle(jobTitle);
        employee.setSalary(salary);
        employee.setHireDate(hireDate);
        employee.setStatus(status);
        employee.setDepartment(department);
        employee.setUser(linkedUser);
        return employeeRepository.save(employee);
    }
}
