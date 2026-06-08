package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.AuthenticationRequest;
import com.ramsai.kitchen.models.dtos.AuthenticationResponse;
import com.ramsai.kitchen.models.dtos.RegisterRequest;
import com.ramsai.kitchen.models.entities.RefreshToken;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse guestLogin() {
        String guestId = UUID.randomUUID().toString().substring(0, 8);
        String username = "GUEST_" + guestId;
        String email = "guest_" + guestId + "@temp.com";
        String password = UUID.randomUUID().toString();

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .role(User.UserRole.GUEST)
                .isActive(true)
                .isEmailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        auditLogService.logAction(savedUser, "GUEST_LOGIN", "SUCCESS", "Guest account created successfully");

        return new AuthenticationResponse(jwtToken, refreshToken.getToken(), savedUser.getUsername(), savedUser.getRole().name());
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(User.UserRole.CUSTOMER)
                .isActive(true)
                .isEmailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());
        
        auditLogService.logAction(savedUser, "REGISTER", "SUCCESS", "User registered successfully");
        
        return new AuthenticationResponse(jwtToken, refreshToken.getToken(), savedUser.getUsername(), savedUser.getRole().name());
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            auditLogService.logFailedLogin(request.username(), "Invalid password");
            throw e;
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.isActive()) {
            auditLogService.logAction(user, "LOGIN", "FAILURE", "Account is inactive");
            throw new RuntimeException("Account is inactive");
        }

        String jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        auditLogService.logAction(user, "LOGIN", "SUCCESS", "User logged in successfully");
        
        return new AuthenticationResponse(jwtToken, refreshToken.getToken(), user.getUsername(), user.getRole().name());
    }

    public AuthenticationResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return new AuthenticationResponse(token, requestRefreshToken, user.getUsername(), user.getRole().name());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
