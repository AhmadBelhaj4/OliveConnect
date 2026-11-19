package tn.oliveconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.oliveconnect.user.document.Role;
import tn.oliveconnect.user.document.UserDocument;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${security.jwt.secret:PtvfsATs53kuMxWNH4kZ3jPyojHoNSs1rumISO5QWYoGamfC/JRMpnvf+Naq1ofp}") String secret,
            @Value("${security.jwt.expiration-seconds:7200}") long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserDocument user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("fullName", user.getFullName());

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + (expirationSeconds * 1000L));

        return Jwts.builder()
                .subject(user.getId())
                .claims(extraClaims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public Role extractRole(String token) {
        String roleStr = extractAllClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
