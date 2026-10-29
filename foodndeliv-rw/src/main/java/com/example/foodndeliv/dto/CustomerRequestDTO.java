package com.example.foodndeliv.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CustomerRequestDTO {

    @NotBlank(message = "Customer name cannot be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address format")
    private String email;
}