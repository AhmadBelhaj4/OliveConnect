package tn.oliveconnect.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Obligatoire") String password) {}
