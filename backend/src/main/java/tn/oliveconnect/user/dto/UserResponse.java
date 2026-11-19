package tn.oliveconnect.user.dto;

import java.time.Instant;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.user.document.UserDocument;

/** Contrat §5.5 : id, fullName, email, role, active, createdAt (PAS de phone, PAS de passwordHash). */
public record UserResponse(String id, String fullName, String email, Role role, boolean active, Instant createdAt) {
    public static UserResponse from(UserDocument u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
