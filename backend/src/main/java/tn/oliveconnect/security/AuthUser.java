package tn.oliveconnect.security;

import tn.oliveconnect.common.enums.Role;

/** L'utilisateur connecté, reconstruit à partir du JWT (principal Spring Security). */
public record AuthUser(String id, String email, Role role) {
    public boolean hasRole(Role r) { return role == r; }
    public boolean isStaff() { return role == Role.COLLECTION_MANAGER || role == Role.ADMIN; }
}
