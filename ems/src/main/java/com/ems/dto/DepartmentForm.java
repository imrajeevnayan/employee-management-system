package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Form backing object for create/edit department. */
@Data
public class DepartmentForm {

    private Long id;

    @NotBlank(message = "Department name is required")
    @Size(max = 80, message = "Name must not exceed 80 characters")
    private String name;

    @NotBlank(message = "Department code is required")
    @Pattern(regexp = "^[A-Za-z0-9]{2,10}$",
             message = "Code must be 2-10 letters or digits (stored uppercase)")
    private String code;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
