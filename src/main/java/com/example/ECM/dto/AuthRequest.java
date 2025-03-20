package com.example.ECM.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    @NotBlank(message = "Username or Email is required")
    private String username;  // Có thể là username hoặc email

    @NotBlank(message = "Password is required")
    private String password;
}