package com.ems.service;

import com.ems.domain.Department;
import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardData loadDashboardData() {
        long total = employeeRepository.count();
        List<DepartmentStat> stats = departmentRepository.findAllByOrderByNameAsc().stream()
                .map(department -> toStat(department, total))
                .toList();
        return new DashboardData(
                total,
                employeeRepository.countByStatus(EmploymentStatus.ACTIVE),
                employeeRepository.countByStatus(EmploymentStatus.ON_LEAVE),
                departmentRepository.count(),
                employeeRepository.findTop5ByOrderByHireDateDesc(),
                stats);
    }

    private DepartmentStat toStat(Department department, long totalEmployees) {
        long count = employeeRepository.countByDepartmentId(department.getId());
        long percentage = totalEmployees == 0 ? 0 : Math.round(count * 100.0 / totalEmployees);
        return new DepartmentStat(department.getName(), count, percentage);
    }

    public record DashboardData(long totalEmployees,
                                long activeEmployees,
                                long onLeaveEmployees,
                                long departmentCount,
                                List<Employee> recentHires,
                                List<DepartmentStat> departmentStats) {
    }

    public record DepartmentStat(String departmentName, long employeeCount, long percentage) {
    }
}
