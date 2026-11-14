package co.istad.a8scommonsecurity;

import co.istad.a8scommonsecurity.converter.CustomJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class A8sCommonSecurityApplicationTests {

    @Test
    void testJwtAuthenticationConverter() {
        CustomJwtAuthenticationConverter converter = new CustomJwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "test-user",
                        "uuid", "123e4567-e89b-12d3-a456-426614174000",
                        "email", "test@a8s.io",
                        "authorities", List.of("ROLE_ADMIN", "cluster:create")
                )
        );

        AbstractAuthenticationToken auth = converter.convert(jwt);
        assertNotNull(auth);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("cluster:create")));
    }
}
