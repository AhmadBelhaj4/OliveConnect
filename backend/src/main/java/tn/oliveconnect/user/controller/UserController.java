package tn.oliveconnect.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.user.dto.UserResponse;
import tn.oliveconnect.user.service.AuthService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService auth;
    public UserController(AuthService auth) { this.auth = auth; }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) { return auth.me(me); }
}
