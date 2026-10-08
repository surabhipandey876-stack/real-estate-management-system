package com.realestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterServletRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/uploads/**").permitAll()
                        .requestMatchers("/api/health", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/properties").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/properties/*").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/customers/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/properties/*/approval").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/properties", "/api/properties/**")
                        .hasAnyRole("ADMIN", "AGENT", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/properties", "/api/properties/**")
                        .hasAnyRole("ADMIN", "AGENT")
                        .requestMatchers(HttpMethod.PUT, "/api/properties/**")
                        .hasAnyRole("ADMIN", "AGENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/properties/**")
                        .hasAnyRole("ADMIN", "AGENT")
                        .requestMatchers("/api/favorites", "/api/favorites/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/inquiries", "/api/inquiries/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/inquiries/*/reply")
                        .hasAnyRole("ADMIN", "AGENT")
                        .requestMatchers(HttpMethod.GET, "/api/inquiries", "/api/inquiries/**")
                        .hasAnyRole("ADMIN", "AGENT", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/visits", "/api/visits/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/visits", "/api/visits/**")
                        .hasAnyRole("ADMIN", "AGENT", "CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/visits/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/status")
                        .hasAnyRole("ADMIN", "AGENT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
