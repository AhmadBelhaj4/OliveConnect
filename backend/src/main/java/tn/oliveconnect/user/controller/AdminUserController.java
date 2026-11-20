package tn.oliveconnect.user.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.audit.service.AuditService;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.dto.AdminUserDtos.*;
import tn.oliveconnect.user.dto.UserResponse;
import tn.oliveconnect.user.repository.UserRepository;
import tn.oliveconnect.user.service.AuthService;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminUserController(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(users.findAll(pageable), UserResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody AdminCreateUserRequest req) {
        String email = AuthService.normalizeEmail(req.email());
        if (users.existsByEmail(email)) throw ApiException.duplicate("email", "Cet email est déjà utilisé.");
        UserDocument u = new UserDocument();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setRole(req.role());
        u.setActive(true);
        u = users.save(u);
        audit.record(admin, "USER_CREATED", "USER", u.getId(), "role=" + u.getRole());
        return UserResponse.from(u);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                   @Valid @RequestBody UpdateRoleRequest req) {
        UserDocument u = users.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
        u.setRole(req.role());
        u = users.save(u);
        audit.record(admin, "ROLE_CHANGED", "USER", u.getId(), "newRole=" + req.role());
        return UserResponse.from(u);
    }

    @PatchMapping("/{id}/active")
    public UserResponse updateActive(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                     @Valid @RequestBody UpdateActiveRequest req) {
        UserDocument u = users.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
        u.setActive(req.active());
        u = users.save(u);
        audit.record(admin, "USER_ACTIVE_CHANGED", "USER", u.getId(), "active=" + req.active());
        return UserResponse.from(u);
    }
}
