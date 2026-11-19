package tn.oliveconnect.user.service;

import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.security.JwtService;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.dto.*;
import tn.oliveconnect.user.repository.UserRepository;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS = "Email ou mot de passe incorrect.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final tn.oliveconnect.security.LoginRateLimiter rateLimiter;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       tn.oliveconnect.security.LoginRateLimiter rateLimiter) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public UserResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmail(email)) {
            throw ApiException.duplicate("email", "Cet email est déjà utilisé.");
        }
        UserDocument u = new UserDocument();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setRole(Role.FARMER);   // imposé : l'inscription publique ne crée que des agriculteurs
        u.setActive(true);
        try {
            return UserResponse.from(users.save(u));
        } catch (DuplicateKeyException e) {   // course entre deux inscriptions simultanées
            throw ApiException.duplicate("email", "Cet email est déjà utilisé.");
        }
    }

    public LoginResponse login(LoginRequest req) {
        return login(req, "127.0.0.1");
    }

    public LoginResponse login(LoginRequest req, String ip) {
        String email = normalizeEmail(req.email());
        rateLimiter.checkAllowed(ip, email);

        var opt = users.findByEmail(email)
                .filter(UserDocument::isActive)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()));

        if (opt.isEmpty()) {
            rateLimiter.recordFailure(ip, email);
            throw ApiException.unauthenticated(BAD_CREDENTIALS);
        }

        rateLimiter.reset(ip, email);
        UserDocument u = opt.get();
        String token = jwt.generate(u.getId(), u.getEmail(), u.getRole());
        return new LoginResponse(token, "Bearer", jwt.expirationSeconds(), UserResponse.from(u));
    }

    public UserResponse me(AuthUser me) {
        if (me == null) {
            throw ApiException.unauthenticated(null);
        }
        return users.findById(me.id()).map(UserResponse::from)
                .orElseThrow(() -> ApiException.unauthenticated(null));
    }
}
