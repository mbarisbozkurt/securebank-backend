package com.securebank.service;

import com.securebank.dto.LoginRequest;
import com.securebank.dto.LoginResponse;
import com.securebank.dto.MessageResponse;
import com.securebank.dto.RegisterRequest;
import com.securebank.model.AuditAction;
import com.securebank.model.Role;
import com.securebank.model.User;
import com.securebank.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.securebank.security.JwtUtil;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuditLogService auditLogService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
        this.refreshTokenService = refreshTokenService;
    }

    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            return new MessageResponse("Registration request received");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.record(AuditAction.USER_REGISTERED, email, "USER", savedUser.getId(), "Public registration");

        return new MessageResponse("Registration request received");
    }

    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        auditLogService.record(AuditAction.USER_LOGIN, user.getEmail(), "USER", user.getId(), "Login succeeded");

        return new AuthResult(
                new LoginResponse(token, "Bearer", jwtUtil.getExpiration()),
                refreshToken
        );
    }

    public AuthResult refresh(String rawRefreshToken) {
        User user = refreshTokenService.consumeAndRotate(rawRefreshToken);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);

        return new AuthResult(
                new LoginResponse(token, "Bearer", jwtUtil.getExpiration()),
                refreshToken
        );
    }

    public MessageResponse logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
        return new MessageResponse("Logout completed");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthResult(LoginResponse response, RefreshTokenService.IssuedRefreshToken refreshToken) {
    }
}
