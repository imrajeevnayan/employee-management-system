package com.ems.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the URL-level RBAC matrix and the login flow against the seeded
 * demo data (see DataSeeder).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EMS")));
    }

    @Test
    void adminLoginSucceedsAndLandsOnDashboard() throws Exception {
        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("admin")
                        .password("Admin@123"))
                .andExpect(SecurityMockMvcResultMatchers.authenticated().withRoles("ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("admin")
                        .password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void hrIsForbiddenFromUserManagement() throws Exception {
        mockMvc.perform(get("/users").with(user("hr.kavya").roles("HR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenUserManagement() throws Exception {
        mockMvc.perform(get("/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void managerCanListEmployees() throws Exception {
        mockMvc.perform(get("/employees").with(user("mgr.arjun").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void plainEmployeeIsRedirectedFromDirectoryToOwnProfile() throws Exception {
        mockMvc.perform(get("/employees").with(user("emp.priya").roles("EMPLOYEE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void employeeCannotViewAnotherEmployeesRecord() throws Exception {
        // Seeded employee #1 (Arjun, Engineering) does not belong to emp.priya.
        mockMvc.perform(get("/employees/1").with(user("emp.priya").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanOpenOwnProfile() throws Exception {
        mockMvc.perform(get("/profile").with(user("emp.priya").roles("EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotReachActuatorBeyondHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void managerFromOtherDepartmentIsForbiddenToEdit() throws Exception {
        // mgr.arjun manages Engineering; seeded employee #6 (Amit) is in Finance.
        mockMvc.perform(get("/employees/6/edit").with(user("mgr.arjun").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    // ---- Page rendering checks (MockMvc renders Thymeleaf views) -------------

    @Test
    void dashboardRendersForAdmin() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("Total employees")));
    }

    @Test
    void employeeDetailRendersForManagerOfSameDepartment() throws Exception {
        mockMvc.perform(get("/employees/1").with(user("mgr.arjun").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Arjun Mehta")))
                .andExpect(content().string(containsString("Engineering")));
    }

    @Test
    void employeeCreateFormRendersForHr() throws Exception {
        mockMvc.perform(get("/employees/new").with(user("hr.kavya").roles("HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add employee")));
    }

    @Test
    void departmentListRendersForManager() throws Exception {
        mockMvc.perform(get("/departments").with(user("mgr.arjun").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Engineering")));
    }
}
