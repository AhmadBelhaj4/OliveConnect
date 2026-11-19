package tn.oliveconnect.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.exception.ConflictException;
import tn.oliveconnect.common.exception.ResourceNotFoundException;
import tn.oliveconnect.security.JwtService;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.dto.AuthResponse;
import tn.oliveconnect.user.dto.LoginRequest;
import tn.oliveconnect.user.dto.RegisterRequest;
import tn.oliveconnect.user.dto.UserResponse;
import tn.oliveconnect.user.repository.UserRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Cet email est déjà utilisé : " + request.getEmail());
        }

        UserDocument user = UserDocument.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UserDocument savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        UserDocument user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect.");
        }

        if (!user.isActive()) {
            throw new BadCredentialsException("Le compte utilisateur est désactivé.");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public UserResponse getUserById(String id) {
        UserDocument user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        return toUserResponse(user);
    }

    public UserResponse toUserResponse(UserDocument user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
