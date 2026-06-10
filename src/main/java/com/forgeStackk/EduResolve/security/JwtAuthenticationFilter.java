package com.forgeStackk.EduResolve.security;

import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil             jwtUtil;
    private final JwtDecoder          keycloakDecoder;
    private final UserLoginRepository userLoginRepo;

    private static final Set<String> INTERNAL_KC_ROLES =
            Set.of("default-roles-eduresolve", "offline_access", "uma_authorization");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("JWT filter: no Bearer token on {} {}", request.getMethod(), request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (tryCustomJwt(token, request)) {
            chain.doFilter(request, response);
            return;
        }

        tryKeycloakJwt(token, request);
        chain.doFilter(request, response);
    }

    // ── Custom HMAC JWT ──────────────────────────────────────────────────────

    private boolean tryCustomJwt(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtUtil.parse(token);
            Long   userId = Long.parseLong(claims.getSubject());
            String role   = claims.get("role", String.class);
            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("JWT filter: custom JWT authenticated userId={} on {}", userId, request.getRequestURI());
            return true;
        } catch (Exception e) {
            log.debug("JWT filter: custom JWT failed on {} — {}", request.getRequestURI(), e.getMessage());
            return false;
        }
    }

    // ── Keycloak RS256 JWT ───────────────────────────────────────────────────

    private void tryKeycloakJwt(String token, HttpServletRequest request) {
        try {
            Jwt    jwt               = keycloakDecoder.decode(token);
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            String email             = jwt.getClaimAsString("email");

            Optional<UserLogin> user = Optional.empty();
            if (preferredUsername != null) user = userLoginRepo.findByUsername(preferredUsername);
            if (user.isEmpty() && email != null) user = userLoginRepo.findByEmail(email);

            if (user.isEmpty()) {
                log.warn("JWT filter: Keycloak token valid but no matching user (sub={}, preferred_username={})",
                        jwt.getSubject(), preferredUsername);
                return;
            }

            var authorities = keycloakRoles(jwt);
            var auth = new UsernamePasswordAuthenticationToken(user.get().getId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("JWT filter: Keycloak JWT authenticated userId={} on {}", user.get().getId(), request.getRequestURI());

        } catch (Exception e) {
            log.warn("JWT filter: Keycloak JWT rejected on {} — {}: {}", request.getRequestURI(),
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> keycloakRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        return roles.stream()
                .filter(r -> !INTERNAL_KC_ROLES.contains(r))
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toList());
    }
}
