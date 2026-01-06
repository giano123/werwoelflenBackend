package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.AuthResponse;
import com.ausganslage.ausgangslageBackend.dto.LoginRequest;
import com.ausganslage.ausgangslageBackend.dto.RegisterRequest;
import com.ausganslage.ausgangslageBackend.dto.UserDto;
import com.ausganslage.ausgangslageBackend.model.Session;
import com.ausganslage.ausgangslageBackend.model.User;
import com.ausganslage.ausgangslageBackend.repository.SessionRepository;
import com.ausganslage.ausgangslageBackend.repository.UserRepository;
import com.ausganslage.ausgangslageBackend.util.AuditLogger;
import com.ausganslage.ausgangslageBackend.util.CodeGenerator;
import com.ausganslage.ausgangslageBackend.util.LoggingContext;
import com.ausganslage.ausgangslageBackend.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registration attempt for username: {}, email: {}", request.getUsername(), request.getEmail());
        LoggingContext.setAction("REGISTER");

        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed - username already exists: {}", request.getUsername());
            AuditLogger.logAuthenticationFailure(request.getUsername(), "Username already exists");
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", request.getEmail());
            AuditLogger.logAuthenticationFailure(request.getUsername(), "Email already exists");
            throw new IllegalArgumentException("Email already exists");
        }

        logger.debug("Creating new user with username: {}", request.getUsername());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(PasswordUtil.hashPassword(request.getPassword()));
        user.setAvatarConfig("default");
        user.setCreatedAt(Instant.now());

        user = userRepository.save(user);

        LoggingContext.setUserId(user.getId());
        LoggingContext.setUsername(user.getUsername());

        logger.info("User created successfully: userId={}, username={}", user.getId(), user.getUsername());
        AuditLogger.logUserRegistration(user.getUsername(), user.getId());

        String token = createSession(user.getId());

        logger.debug("Session created for new user: userId={}", user.getId());

        return new AuthResponse(token, toUserDto(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for: {}", request.getUsernameOrEmail());
        LoggingContext.setAction("LOGIN");

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> {
                    logger.warn("Login failed - user not found: {}", request.getUsernameOrEmail());
                    AuditLogger.logAuthenticationFailure(request.getUsernameOrEmail(), "User not found");
                    return new IllegalArgumentException("Invalid credentials");
                });

        LoggingContext.setUserId(user.getId());
        LoggingContext.setUsername(user.getUsername());

        if (!PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed - invalid password for user: userId={}, username={}",
                user.getId(), user.getUsername());
            AuditLogger.logAuthenticationFailure(user.getUsername(), "Invalid password");
            throw new IllegalArgumentException("Invalid credentials");
        }

        logger.info("User authenticated successfully: userId={}, username={}", user.getId(), user.getUsername());

        String token = createSession(user.getId());

        AuditLogger.logUserLogin(user.getUsername(), user.getId(), token);
        logger.debug("Session created for user: userId={}, username={}", user.getId(), user.getUsername());

        return new AuthResponse(token, toUserDto(user));
    }

    private String createSession(Long userId) {
        logger.debug("Creating new session for userId: {}", userId);

        String token = CodeGenerator.generateToken();
        Session session = new Session();
        session.setUserId(userId);
        session.setToken(token);
        session.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        session.setCreatedAt(Instant.now());
        sessionRepository.save(session);

        logger.debug("Session created successfully: userId={}, expiresAt={}",
            userId, session.getExpiresAt());

        return token;
    }

    public UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatarConfig(user.getAvatarConfig());
        return dto;
    }
}

