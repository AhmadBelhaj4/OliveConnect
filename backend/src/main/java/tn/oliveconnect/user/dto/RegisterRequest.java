package tn.oliveconnect.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Obligatoire") @Size(max = 100, message = "100 caractères maximum") String fullName,
        @NotBlank(message = "Obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Obligatoire") @Size(min = 8, message = "8 caractères minimum") String password,
        @Size(max = 30, message = "30 caractères maximum") String phone) {

    public RegisterRequest {
        if (email != null) email = email.trim();
        if (fullName != null) fullName = fullName.trim();
        if (phone != null) phone = phone.trim();
    }
}
