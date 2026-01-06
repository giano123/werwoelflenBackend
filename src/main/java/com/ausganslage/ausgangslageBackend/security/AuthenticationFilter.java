package com.ausganslage.ausgangslageBackend.security;

import com.ausganslage.ausgangslageBackend.model.Session;
import com.ausganslage.ausgangslageBackend.model.User;
import com.ausganslage.ausgangslageBackend.repository.SessionRepository;
import com.ausganslage.ausgangslageBackend.repository.UserRepository;
import com.ausganslage.ausgangslageBackend.util.LoggingContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AuthenticationFilter(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = request.getRemoteAddr();

        LoggingContext.setIpAddress(ipAddress);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.trace("OPTIONS request - skipping authentication");
            filterChain.doFilter(request, response);
            LoggingContext.clear();
            return;
        }

        if (path.startsWith("/api/auth/register") || path.startsWith("/api/auth/login")) {
            filterChain.doFilter(request, response);
            LoggingContext.clear();
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            LoggingContext.setSessionToken(token);

            Optional<Session> sessionOpt = sessionRepository.findByToken(token);

            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                logger.trace("Session found for token, checking expiration");

                if (session.getExpiresAt().isAfter(Instant.now())) {
                    Optional<User> userOpt = userRepository.findById(session.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        LoggingContext.setUserId(user.getId());
                        LoggingContext.setUsername(user.getUsername());


                        request.setAttribute("currentUser", user);
                        filterChain.doFilter(request, response);
                        LoggingContext.clear();
                        return;
                    } else {
                        logger.warn("Session exists but user not found: userId={}", session.getUserId());
                    }
                } else {
                    logger.trace("Session expired for token");
                }
            } else {
                logger.trace("No session found for provided token");
            }
        } else {
            logger.trace("No Authorization header or invalid format for path: {}", path);
        }

        if (!path.startsWith("/api/")) {
            logger.trace("Non-API path, allowing access: {}", path);
            filterChain.doFilter(request, response);
            LoggingContext.clear();
            return;
        }

        logger.warn("Unauthorized access attempt: path={}, ip={}", path, ipAddress);
        LoggingContext.clear();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
}

