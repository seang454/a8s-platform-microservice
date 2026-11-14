package co.istad.iam_service.security;


import co.istad.iam_service.domain.Role;
import co.istad.iam_service.domain.User;
import co.istad.iam_service.feature.oauth2.JpaRegisteredClientRepository;
import co.istad.iam_service.feature.role.RoleRepository;
import co.istad.iam_service.feature.user.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityInit {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JpaRegisteredClientRepository jpaRegisteredClientRepository;

    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            List<User> users = new ArrayList<>();
            User user = new User();
            user.setUuid(UUID.randomUUID().toString());
            user.setUsername("dara");
            user.setPassword(passwordEncoder.encode("qwer"));
            user.setEmail("sisovandara@gmail.com");
            user.setDob(LocalDate.of(2004, 10, 8));
            user.setGender("Male");
            user.setProfileImage("https://i.pinimg.com/1200x/87/17/e1/8717e10f1de9cece362988c51616a4cd.jpg");
            user.setCoverImage("https://i.pinimg.com/1200x/87/17/e1/8717e10f1de9cece362988c51616a4cd.jpg");
            user.setFamilyName("Kong");
            user.setGivenName("Sisovandara");
            user.setPhoneNumber("017349304");
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);
            user.setIsEnabled(true);

            // Assign role to user
            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName("SUPER_ADMIN"));
            roles.add(roleRepository.findByName("USER"));
            user.setRoles(roles);

            User user1 = new User();
            user1.setUuid(UUID.randomUUID().toString());
            user1.setUsername("seang");
            user1.setPassword(passwordEncoder.encode("seang0405"));
            user1.setEmail("pengseangsim@gmail.com");
            user1.setDob(LocalDate.of(2004, 10, 8));
            user1.setGender("Male");
            user1.setProfileImage("https://i.pinimg.com/1200x/87/17/e1/8717e10f1de9cece362988c51616a4cd.jpg");
            user1.setCoverImage("https://i.pinimg.com/1200x/87/17/e1/8717e10f1de9cece362988c51616a4cd.jpg");
            user1.setFamilyName("PengSeang");
            user1.setGivenName("Sim");
            user1.setPhoneNumber("016901360");
            user1.setAccountNonExpired(true);
            user1.setAccountNonLocked(true);
            user1.setCredentialsNonExpired(true);
            user1.setIsEnabled(true);

            // Assign role to user
            Set<Role> roles1 = new HashSet<>();
            roles1.add(roleRepository.findByName("SUPER_ADMIN"));
            roles1.add(roleRepository.findByName("USER"));
            user1.setRoles(roles1);

            users.add(user);
            users.add(user1);
            userRepository.saveAll(users);
            log.info("Users have been saved: {}, {}", user.getId(), user1.getId());
        }
    }

    @PostConstruct
    void initOAuth2() {

        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofDays(3))
                .reuseRefreshTokens(false) // refresh token rotation
                .refreshTokenTimeToLive(Duration.ofDays(5))
                .build();

        ClientSettings clientSettings = ClientSettings.builder()
                .requireProofKey(false)
                .requireAuthorizationConsent(false)
                .build();

        var itpStandard = RegisteredClient.withId("itp-standard")
                .clientId("itp-standard")
                .clientSecret(passwordEncoder.encode("qwerqwer")) // store in secret manager
                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID); // required!
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add(OidcScopes.EMAIL);
                })
                .redirectUris(uris -> {
                    uris.add("http://localhost:9090/login/oauth2/code/itp-standard");
                    uris.add("http://localhost:9999/login/oauth2/code/itp-standard");
                    uris.add("http://localhost:9090");
                    uris.add("https://cstad.edu.kh/");
                })
                .postLogoutRedirectUris(uris -> {
                    uris.add("http://localhost:9090");
                })
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)//TODO: grant_type:client_credentials, client_id & client_secret, redirect_uri
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                    grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                })
                .clientSettings(clientSettings)
                .tokenSettings(tokenSettings)
                .build();

        var itpFrontBff = RegisteredClient.withId("itp-frontbff")
                .clientId("itp-frontbff")
                .clientSecret(passwordEncoder.encode("qwerqwer")) // store in secret manager
                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID); // required!
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add(OidcScopes.EMAIL);
                })

                // I authorize my IAM server to redirect the user to these specific URLs after they log in.
                // =========================================================================
                // Authorized OAuth2 Redirect URIs (Whitelist)
                // -------------------------------------------------------------------------
                // Security Purpose:
                // Prevents Open Redirector attacks by strictly whitelisting where the IAM
                // server is allowed to send the authorization code and user after login.
                //
                // 1. /login/oauth2/code/itp-frontbff:
                //    - The primary OAuth2 Callback endpoint on front-bff (port :9990).
                //    - Immediately after successful login on :9090, the IAM redirects here
                //      with '?code=...', allowing Spring Security on the BFF to exchange
                //      it for the real JWT access/refresh tokens.
                //    - MUST match character-for-character with front-bff's application.yaml.
                //
                // 2. http://localhost:9990:
                //    - The post-login base landing page and post-logout redirect destination.
                // =========================================================================
                .redirectUris(uris -> {
                    uris.add("http://localhost:9990/login/oauth2/code/itp-frontbff");
                    uris.add("http://localhost:9990");
                })
                .postLogoutRedirectUris(uris -> {
                    uris.add("http://localhost:9990");
                })
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)//TODO: grant_type:client_credentials, client_id & client_secret, redirect_uri
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                })
                .clientSettings(clientSettings)
                .tokenSettings(tokenSettings)
                .build();

        var itpAdminBff = RegisteredClient.withId("itp-adminbff")
                .clientId("itp-adminbff")
                .clientSecret(passwordEncoder.encode("qwerqwer")) // store in secret manager
                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID); // required!
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add(OidcScopes.EMAIL);
                })
                .redirectUris(uris -> {
                    uris.add("http://localhost:9991/login/oauth2/code/itp-adminbff");
                    uris.add("http://localhost:9991");
                })

                // This the {baseUrl} that we use to Redirect to this URL "http://localhost:9991" when do the OIDC logout after
                .postLogoutRedirectUris(uris -> {
                    uris.add("http://localhost:9991");
                })
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)//TODO: grant_type:client_credentials, client_id & client_secret, redirect_uri
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                })
                .clientSettings(clientSettings)
                .tokenSettings(tokenSettings)
                .build();

        RegisteredClient registeredClient = jpaRegisteredClientRepository.findByClientId("itp-standard");
        log.info("Registered client: {}", registeredClient);

        RegisteredClient registeredClient2 = jpaRegisteredClientRepository.findByClientId("itp-frontbff");
        log.info("Registered client 2: {}", registeredClient2);

        RegisteredClient registeredClient3 = jpaRegisteredClientRepository.findByClientId("itp-adminbff");
        log.info("Registered client 3: {}", registeredClient3);

        if (registeredClient == null) {
            jpaRegisteredClientRepository.save(itpStandard);
        }

        if (registeredClient2 == null) {
            jpaRegisteredClientRepository.save(itpFrontBff);
        }

        if (registeredClient3 == null) {
            jpaRegisteredClientRepository.save(itpAdminBff);
        }

    }

}
