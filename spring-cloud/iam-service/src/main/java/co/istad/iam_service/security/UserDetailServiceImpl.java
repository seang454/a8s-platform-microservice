//package co.istad.itpidentityservice.security;
//
//import co.istad.itpidentityservice.domain.Role;
//import co.istad.itpidentityservice.domain.User;
//import co.istad.itpidentityservice.features.user.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class UserDetailServiceImpl implements UserDetailsService {
//    private final UserRepository userRepository;
//
//
//    // Override default
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User userLogged = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        // Build UserDetails object
//        String[] roles = userLogged.getRoles().stream()
//                .map(Role::getName)
//                .toArray(String[]::new);
//
////        Set<GrantedAuthority> authorities = userLogged.getRoles()
////                .stream()
////                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
////                .collect(Collectors.toSet());
//
//        // Use forEach and get permission as well
//        List<GrantedAuthority> authorities = new ArrayList<>();
//        userLogged.getRoles().forEach(role -> {
//            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
//            role.getPermissions().forEach(permission -> {
//                authorities.add(new SimpleGrantedAuthority(permission.getName()));
//            });
//        });
//
//
//        UserDetails userSecurity = org.springframework.security.core.userdetails.User.builder()
//                .username(userLogged.getUsername())
//                .password(userLogged.getPassword())
//                //.roles(roles)
//                .authorities(authorities)
//                .build();
//        log.info("UserDetailsServiceImpl loadUserByUsername = {}", userSecurity.getAuthorities());
//        log.info("UserDetailsServiceImpl loadUserByUsername = {}", userSecurity.getUsername());
//        return userSecurity;
//    }
//}


package co.istad.iam_service.security;


import co.istad.iam_service.domain.User;
import co.istad.iam_service.feature.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User userLogged = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Collect authorities (roles and permissions)
        List<GrantedAuthority> authorities = new ArrayList<>();
        userLogged.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        });

        // Build CustomUserDetails with additional user information
        CustomUserDetails customUserDetails = new CustomUserDetails(
                userLogged.getUsername(),
                userLogged.getPassword(),
                userLogged.getIsEnabled(),
                userLogged.getAccountNonExpired(),
                userLogged.getCredentialsNonExpired(),
                userLogged.getAccountNonLocked(),
                authorities,
                userLogged.getUuid(),
                userLogged.getEmail(),
                userLogged.getFamilyName(),
                userLogged.getGivenName(),
                userLogged.getPhoneNumber(),
                userLogged.getProfileImage(),
                userLogged.getGender()
        );

        log.info("CustomUserDetails loaded for user: {}", customUserDetails.getUsername());
        log.info("User authorities: {}", customUserDetails.getAuthorities());

        return customUserDetails;
    }

    /**
     By writing:

     @Bean: You tell Spring: "Spring, I will give you a worker object to manage."
     public OAuth2TokenCustomizer<...> tokenCustomizer(): You promise Java: "This method will RETURN an object that implements OAuth2TokenCustomizer."

     Because this method does NOT say public void tokenCustomizer(), Java forces you to use the keyword return.

     * */

//       Way A: Without Lambda (Old Java) using Anonymous inner class
//        @Bean
//        public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
//            // 1. Create the worker implementing the interface:
//            OAuth2TokenCustomizer<JwtEncodingContext> worker = new OAuth2TokenCustomizer<>() {
//                @Override
//                public void customize(JwtEncodingContext context) {
//                        context.getClaims()
//                        .claim("uuid", userDetails.getUuid())
//                        .claim("email", userDetails.getEmail())
//                        .claim("family_name", userDetails.getFamilyName())
//                        .claim("given_name", userDetails.getGivenName())
//                        .claim("name", userDetails.getGivenName() + " " + userDetails.getFamilyName())
//                        .claim("phone_number", userDetails.getPhoneNumber())
//                        .claim("profile_image", userDetails.getProfileImage())
//                        .claim("gender", userDetails.getGender())
//                        .claim("authorities", userDetails.getAuthorities().stream()
//                        .map(a -> a.getAuthority())
//                        .collect(Collectors.toList()));
//                }
//            };
//
//            // 2. Return the worker to Spring:
//            return worker;
//        }


//       Way B: With Lambda
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(){



        return context -> {
            Authentication principal = context.getPrincipal();

            if (!(principal.getPrincipal() instanceof CustomUserDetails userDetails)) {
                return;
            }

            // Add custom claims to ID Token
            log.info("Adding custom claims to ID token for user: {}", userDetails.getUsername());

            context.getClaims()
                    .claim("uuid", userDetails.getUuid())
                    .claim("email", userDetails.getEmail())
                    .claim("family_name", userDetails.getFamilyName())
                    .claim("given_name", userDetails.getGivenName())
                    .claim("name", userDetails.getGivenName() + " " + userDetails.getFamilyName())
                    .claim("phone_number", userDetails.getPhoneNumber())
                    .claim("profile_image", userDetails.getProfileImage())
                    .claim("gender", userDetails.getGender())
                    .claim("authorities", userDetails.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .collect(Collectors.toList()));

        };
    }
}
