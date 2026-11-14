package co.istad.iam_service.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final OidcUserInfoService oidcUserInfoService;

    @Value("${spring.security.oauth2.authorizationserver.issuer}")
    private String issuerUri;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings
                .builder()
                .issuer(issuerUri)
                .build();
    }

    /**
     * =========================================================================================
     * FILTER CHAIN 1: OAuth2 & OpenID Connect (OIDC) Protocol Chain (Evaluated FIRST: @Order(1))
     * =========================================================================================
     * Target Audience: MACHINES & PROTOCOL CLIENTS (Gateway, BFF, Postman, Frontend Auth).
     *
     * How it works:
     * - securityMatcher(endpointsMatcher): ONLY intercepts official OAuth2 / OIDC endpoints:
     *     • /oauth2/authorize  -> Initiates login & issues authorization code
     *     • /oauth2/token      -> Exchanges code or client_secret for JWT access & refresh tokens
     *     • /oauth2/jwks       -> Exposes Public Key so Gateway/Microservices can verify JWTs
     *     • /userinfo          -> Returns user profile claims mapped by oidcUserInfoService
     *     • /.well-known/**    -> Publishes OpenID configuration metadata
     *
     * - If an unauthenticated user hits an OAuth2 endpoint in a browser (e.g., /oauth2/authorize),
     *   the LoginUrlAuthenticationEntryPoint redirects them to the human login form (/login).
     *
     * - Any non-OAuth2 request (e.g. /login, /css/**, /home) skips this chain and falls into Chain 2.
     * =========================================================================================
     */
    @Bean
    @Order(1)
    public SecurityFilterChain configureOAuth2(HttpSecurity http) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .oidc(oidc -> oidc
                                        .userInfoEndpoint(userInfo -> userInfo
                                                .userInfoMapper(context -> {
                                                    String username = context.getAuthorization()
                                                            .getPrincipalName();
                                                    return oidcUserInfoService.loadUser(username);
                                                })
                                        )
                                )
                )
                .authorizeHttpRequests((authorize) ->
                        authorize.anyRequest().authenticated()
                );

        http
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    /**
     * =========================================================================================
     * FILTER CHAIN 2: Human Web UI & Application Security (Evaluated SECOND: @Order(2))
     * =========================================================================================
     * Target Audience: HUMAN USERS IN WEB BROWSERS.
     *
     * How it works:
     * - Acts as the fallback security chain for all standard web traffic NOT matched by Chain 1.
     * - Serves the custom login HTML page (GET /login) and processes credentials (POST /login).
     * - Authenticates users against PostgreSQL via UserDetailServiceImpl and creates session cookie.
     * - Publicly permits static assets (/css/**, /js/**, /images/**, /favicon.ico, *.map).
     * - Manages user logout (/logout) and safe session caching (requestCache).
     *
     * Collaboration with Chain 1:
     * Once the user logs in here, Spring hands control back to Chain 1 (/oauth2/authorize)
     * so Chain 1 can generate the authorization code and complete the login!
     * =========================================================================================
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/login/oauth2/**", "/resources/**", "/oauth2/login/**", "/public/**").permitAll()
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/favicon.ico",
                                "/.well-known/**",
                                "/sourceMap/**",
                                "/**/*.map",
                                "/**/*.json",
                                "/resources/**",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated())

                // ==============================================================================
                // Request Cache Best Practice:
                // When an unauthenticated user hits a protected URL, Spring Security caches that
                // URL in the session as the 'SavedRequest' to redirect them back after login.
                //
                // By default, Spring caches ANY unauthenticated GET request. This causes bugs when
                // browser extensions (e.g., QuillBot, Grammarly), devtools source maps (*.map),
                // favicons, or background AJAX calls run in the background and overwrite the user's
                // intended destination (causing unwanted redirects to fonts, .css.map, or JSON).
                //
                // Setting MediaTypeRequestMatcher(TEXT_HTML) ensures Spring ONLY caches real
                // human page navigation (Accept: text/html), guaranteeing a smooth redirect back
                // to the Gateway / original application after successful login.
                // ==============================================================================
                // This avoid redirect to the different URL after completed login like
                // http://localhost:9999/customer/api/customers  logged in http://localhost:9090/sourceMap/assets/css/gdocs-overrides.css.map?continue
                .requestCache(cache -> {
                    HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
                    MediaTypeRequestMatcher htmlMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
                    // Crucial: Ignore '*/*' so background browser tools (DevTools, extensions) are not treated as HTML
                    htmlMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));
                    requestCache.setRequestMatcher(htmlMatcher);
                    cache.requestCache(requestCache);
                })
                .formLogin(form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/home", false)
                                .failureUrl("/login?error=true")
                                .permitAll()
                )
                .logout(logout -> logout
//                        // 👇 THIS tells LogoutFilter to accept GET /logout:
//                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )
                .cors(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // ============== JWT/JWK Configuration ==============

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}



//package co.istad.itpidentityservice.security;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.MediaType;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
//import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//    private final UserDetailsService userDetailsService;
//    private final PasswordEncoder passwordEncoder;
//    @Value("${spring.security.oauth2.authorizationserver.issuer}")
//    private String issuerUri;
//
//    @Bean
//    public DaoAuthenticationProvider daoAuthenticationProvider() {
//        DaoAuthenticationProvider provider =
//                new DaoAuthenticationProvider(userDetailsService);
//        provider.setPasswordEncoder(passwordEncoder);
//
//        return provider;
//    }
//
//
//    @Bean
//    AuthorizationServerSettings authorizationServerSettings() {
//        return AuthorizationServerSettings
//                .builder()
//                .issuer(issuerUri)
//                .build();
//    }
//
//
//    @Bean
//    @Order(1)
//    public SecurityFilterChain configureOAuth2(HttpSecurity http) throws Exception {
//
//        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
//                new OAuth2AuthorizationServerConfigurer();
//
//        http
//                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
//                .with(authorizationServerConfigurer, (authorizationServer) ->
//                        authorizationServer
//                                .oidc(Customizer.withDefaults())	// Initialize `OidcConfigurer`
//                )
//                .authorizeHttpRequests((authorize) ->
//                        authorize.anyRequest().authenticated()
//                );
//
//        http
//                .exceptionHandling(ex -> ex
//                        .defaultAuthenticationEntryPointFor(
//                                new LoginUrlAuthenticationEntryPoint("/login"),
//                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
//                        )
//                );
//
//
//        return http.build();
//    }
//
//
//    @Bean
//    @Order(2)
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(request -> request
//                        .requestMatchers("/login/oauth2/**", "/resources/**", "/oauth2/login/**", "/public/**").permitAll()
//                        .requestMatchers(
//                                "/login",
//                                "/error",
//                                "/resources/**",
//                                "/static/**",
//                                "/css/**",
//                                "/js/**",
//                                "/images/**",
//                                "/webjars/**"
//                        ).permitAll()
//                        .anyRequest().authenticated())
//
//                .formLogin(form -> form
//                                .loginPage("/login")
//                                .loginProcessingUrl("/login")
////                        .defaultSuccessUrl("/", true)
//                                .failureUrl("/login?error=true")
//                                .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/login?logout=true")
//                        .permitAll()
//                )
//
//                .csrf(csrf -> csrf
//                        .ignoringRequestMatchers("/api/**")
//                )
//                .cors(AbstractHttpConfigurer::disable);
//
//        return http.build();
//    }
//
//
//}

