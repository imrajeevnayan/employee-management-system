package com.ems.web;

import com.ems.domain.Employee;
import com.ems.domain.EmploymentStatus;
import com.ems.domain.RoleType;
import com.ems.domain.User;
import com.ems.dto.EmployeeForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.service.DepartmentService;
import com.ems.service.EmployeeService;
import com.ems.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final UserService userService;

    @ModelAttribute("statuses")
    public EmploymentStatus[] statuses() {
        return EmploymentStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) EmploymentStatus status,
                       @RequestParam(required = false) String sort,
                       @PageableDefault(size = 8, sort = "lastName") Pageable pageable,
                       Model model) {

        User current = userService.getCurrentUser();
        boolean directoryViewer = current.hasRole(RoleType.ADMIN)
                || current.hasRole(RoleType.HR)
                || current.hasRole(RoleType.MANAGER);
        if (!directoryViewer) {
            return "redirect:/profile";
        }

        String term = normalize(q);
        Long deptId = (departmentId == null || departmentId <= 0) ? null : departmentId;

        Page<Employee> page = employeeService.search(term, deptId, status, pageable);

        model.addAttribute("page", page);
        model.addAttribute("q", term);
        model.addAttribute("departmentId", deptId);
        model.addAttribute("status", status);
        model.addAttribute("sort", (sort == null || sort.isBlank()) ? "lastName" : sort);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("canManageAll", current.hasRole(RoleType.ADMIN) || current.hasRole(RoleType.HR));
        model.addAttribute("managerDepartmentId", managerDepartmentId(current));
        return "employees/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User current = userService.getCurrentUser();
        Employee employee = employeeService.findById(id);
        employeeService.assertCanView(employee, current);
        model.addAttribute("employee", employee);
        model.addAttribute("canManageAll", current.hasRole(RoleType.ADMIN) || current.hasRole(RoleType.HR));
        model.addAttribute("managerDepartmentId", managerDepartmentId(current));
        return "employees/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String createForm(Model model) {
        populateFormModel(model, new EmployeeForm());
        return "employees/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String create(@Valid @ModelAttribute("form") EmployeeForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form);
            return "employees/form";
        }
        try {
            employeeService.create(form);
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            populateFormModel(model, form);
            return "employees/form";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Employee " + form.getFirstName() + " " + form.getLastName() + " created successfully");
        return "redirect:/employees";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public String editForm(@PathVariable Long id, Model model) {
        User current = userService.getCurrentUser();
        Employee employee = employeeService.findById(id);
        employeeService.assertCanManage(employee, current);
        populateFormModel(model, toForm(employee));
        return "employees/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") EmployeeForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        User current = userService.getCurrentUser();
        employeeService.assertCanManage(employeeService.findById(id), current);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form);
            return "employees/form";
        }
        try {
            employeeService.update(id, form);
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            populateFormModel(model, form);
            return "employees/form";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Employee " + form.getFirstName() + " " + form.getLastName() + " updated successfully");
        return "redirect:/employees/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Employee employee = employeeService.findById(id);
        employeeService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Employee " + employee.getFullName() + " deleted");
        return "redirect:/employees";
    }

    private void populateFormModel(Model model, EmployeeForm form) {
        model.addAttribute("form", form);
        model.addAttribute("departments", departmentService.findAll());
    }

    private EmployeeForm toForm(Employee employee) {
        EmployeeForm form = new EmployeeForm();
        form.setId(employee.getId());
        form.setEmployeeCode(employee.getEmployeeCode());
        form.setFirstName(employee.getFirstName());
        form.setLastName(employee.getLastName());
        form.setEmail(employee.getEmail());
        form.setPhone(employee.getPhone());
        form.setJobTitle(employee.getJobTitle());
        form.setSalary(employee.getSalary());
        form.setHireDate(employee.getHireDate());
        form.setStatus(employee.getStatus());
        form.setDepartmentId(employee.getDepartment().getId());
        return form;
    }

    private String normalize(String q) {
        return (q == null || q.isBlank()) ? null : q.trim();
    }

    /**
     * Department id used to scope the "Edit" buttons shown to a manager;
     * null means "not a scoped manager" (admins/HR are handled by canManageAll).
     */
    private Long managerDepartmentId(User current) {
        if (current.hasRole(RoleType.ADMIN) || current.hasRole(RoleType.HR)) {
            return null;
        }
        if (current.hasRole(RoleType.MANAGER) && current.getEmployee() != null) {
            return current.getEmployee().getDepartment().getId();
        }
        return null;
    }
}
