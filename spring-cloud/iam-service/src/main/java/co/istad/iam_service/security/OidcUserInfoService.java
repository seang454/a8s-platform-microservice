package co.istad.iam_service.security;

import co.istad.iam_service.feature.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OidcUserInfoService {

    private final UserRepository userRepository;

    public OidcUserInfo loadUser(String username) {
        log.info("Loading user info for: {}", username);

        return userRepository.findByUsername(username)
                .map(user -> {
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("sub", user.getUsername());
                    claims.put("preferred_username", user.getUsername());
                    claims.put("uuid", user.getUuid());
                    claims.put("email", user.getEmail());
                    claims.put("email_verified", true);
                    claims.put("family_name", user.getFamilyName());
                    claims.put("given_name", user.getGivenName());
                    claims.put("name", user.getGivenName() + " " + user.getFamilyName());
                    claims.put("phone_number", user.getPhoneNumber());
                    claims.put("phone_number_verified", true);
                    claims.put("profile_image", user.getProfileImage());
                    claims.put("picture", user.getProfileImage());
                    claims.put("gender", user.getGender());

                    var authorities = user.getRoles().stream()
                            .flatMap(role -> role.getPermissions().stream())
                            .map(permission -> permission.getName())
                            .collect(Collectors.toList());
                    claims.put("authorities", authorities);

                    log.info("User info loaded successfully: {}", claims);
                    return new OidcUserInfo(claims);
                })
                .orElse(null);
    }
}