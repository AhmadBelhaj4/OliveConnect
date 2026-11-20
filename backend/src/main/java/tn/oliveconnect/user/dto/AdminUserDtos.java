package tn.oliveconnect.user.dto;

import jakarta.validation.constraints.*;
import tn.oliveconnect.common.enums.Role;

public final class AdminUserDtos {
    private AdminUserDtos() {}
    public record AdminCreateUserRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @Size(max = 30) String phone,
            @NotNull Role role) {}
    public record UpdateRoleRequest(@NotNull Role role) {}
    public record UpdateActiveRequest(@NotNull Boolean active) {}
}
