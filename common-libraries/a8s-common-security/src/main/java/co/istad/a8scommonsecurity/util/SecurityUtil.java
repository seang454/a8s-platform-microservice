package co.istad.a8scommonsecurity.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility helper to easily retrieve current authenticated user details across any A8S microservice.
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Get the active Spring Security Authentication object.
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Get the active Jwt token from the authentication context.
     */
    public static Optional<Jwt> getJwt() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

    /**
     * Extract the user's UUID from the JWT token.
     */
    public static Optional<String> getCurrentUserUuid() {
        return getJwt().map(jwt -> {
            String uuid = jwt.getClaimAsString("uuid");
            return (uuid != null && !uuid.isBlank()) ? uuid : jwt.getSubject();
        });
    }

    /**
     * Extract the user's username/preferred_username.
     */
    public static Optional<String> getCurrentUsername() {
        return getJwt().map(jwt -> {
            String username = jwt.getClaimAsString("preferred_username");
            return (username != null && !username.isBlank()) ? username : jwt.getSubject();
        });
    }

    /**
     * Extract the user's email claim.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getJwt().map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Extract all granted authorities (roles + permissions).
     */
    public static List<String> getCurrentAuthorities() {
        return getAuthentication()
                .map(Authentication::getAuthorities)
                .orElse(Collections.emptyList())
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * Check if the authenticated user has a specific role (e.g. "ADMIN" or "ROLE_ADMIN").
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentAuthorities().contains(roleWithPrefix);
    }

    /**
     * Check if the authenticated user has a specific permission/authority (e.g. "cluster:create").
     */
    public static boolean hasAuthority(String authority) {
        return getCurrentAuthorities().contains(authority);
    }

    /**
     * Retrieve all raw claims from the JWT.
     */
    public static Map<String, Object> getAllClaims() {
        return getJwt().map(Jwt::getClaims).orElse(Collections.emptyMap());
    }

    /**
     * Check if the caller is authenticated.
     */
    public static boolean isAuthenticated() {
        return getAuthentication()
                .map(Authentication::isAuthenticated)
                .orElse(false);
    }
}
