package com.ems.repository;

import com.ems.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "employee", "employee.department"})
    Optional<User> findWithRolesAndEmployeeByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "employee"})
    List<User> findAllWithRolesAndEmployeeByOrderByUsernameAsc();

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
