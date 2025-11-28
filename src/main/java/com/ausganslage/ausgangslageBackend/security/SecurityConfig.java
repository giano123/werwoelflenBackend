package com.ausganslage.ausgangslageBackend.security;

import com.ausganslage.ausgangslageBackend.repository.SessionRepository;
import com.ausganslage.ausgangslageBackend.repository.UserRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationFilter authenticationFilter(SessionRepository sessionRepository, UserRepository userRepository) {
        return new AuthenticationFilter(sessionRepository, userRepository);
    }

    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration(AuthenticationFilter filter) {
        FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }
}

