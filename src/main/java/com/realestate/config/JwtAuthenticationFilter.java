package com.realestate.config;

import com.realestate.model.RoleName;
import com.realestate.service.JwtService;
import com.realestate.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtService.TokenIdentity identity = jwtService.parseToken(authorization.substring(7));
            var account = userRepository.findByEmailIgnoreCaseWithRole(identity.email())
                    .filter(user -> user.isActive())
                    .orElse(null);
            if (account == null) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account is unavailable");
                return;
            }
            RoleName role = account.getRole().getName();
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    identity.email(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired bearer token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
