package com.ems.web;

import com.ems.dto.DepartmentForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAllWithCounts());
        return "departments/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("department", departmentService.findById(id));
        model.addAttribute("employees", departmentService.employeesOf(id));
        return "departments/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String createForm(Model model) {
        model.addAttribute("form", new DepartmentForm());
        return "departments/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String create(@Valid @ModelAttribute("form") DepartmentForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "departments/form";
        }
        try {
            departmentService.create(form);
        } catch (DuplicateResourceException ex) {
            bindingResult.reject("duplicate", ex.getMessage());
            return "departments/form";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Department '" + form.getName() + "' created successfully");
        return "redirect:/departments";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String editForm(@PathVariable Long id, Model model) {
        var department = departmentService.findById(id);
        DepartmentForm form = new DepartmentForm();
        form.setId(department.getId());
        form.setName(department.getName());
        form.setCode(department.getCode());
        form.setLocation(department.getLocation());
        form.setDescription(department.getDescription());
        model.addAttribute("form", form);
        return "departments/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") DepartmentForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "departments/form";
        }
        try {
            departmentService.update(id, form);
        } catch (DuplicateResourceException ex) {
            bindingResult.reject("duplicate", ex.getMessage());
            return "departments/form";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Department '" + form.getName() + "' updated successfully");
        return "redirect:/departments";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/departments";
    }
}
