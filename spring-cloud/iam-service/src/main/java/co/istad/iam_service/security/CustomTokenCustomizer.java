package co.istad.iam_service.security; ////package co.istad.itpidentityservice.security;
////
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.security.core.Authentication;
////import org.springframework.security.core.GrantedAuthority;
////import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
////import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
////import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
////import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
////import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
////import org.springframework.stereotype.Component;
////
////import java.util.stream.Collectors;
////
////@Component
////@Slf4j
////public class CustomTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
////
////    @Override
////    public void customize(JwtEncodingContext context) {
////        log.info("=== Token Customizer Called ===");
////        log.info("Token Type: {}", context.getTokenType().getValue());
////
////        Authentication principal = context.getPrincipal();
////        log.info("Principal: {}", principal);
////        log.info("Principal class: {}", principal.getClass().getName());
////        log.info("Principal.getPrincipal() class: {}", principal.getPrincipal().getClass().getName());
////
////        Object principalObj = principal.getPrincipal();
////
////        if (!(principalObj instanceof CustomUserDetails)) {
////            log.warn("Principal is NOT CustomUserDetails, it is: {}", principalObj.getClass().getName());
////            return;
////        }
////
////        CustomUserDetails userDetails = (CustomUserDetails) principalObj;
////        log.info("CustomUserDetails found - Username: {}, UUID: {}, Email: {}",
////                userDetails.getUsername(), userDetails.getUuid(), userDetails.getEmail());
////
////        // For ID Token
////        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
////            log.info("=== Customizing ID Token ===");
////
////            context.getClaims()
////                    .claim("sub", userDetails.getUsername())
////                    .claim("preferred_username", userDetails.getUsername())
////                    .claim("uuid", userDetails.getUuid())
////                    .claim("email", userDetails.getEmail())
////                    .claim("email_verified", true)
////                    .claim("family_name", userDetails.getFamilyName())
////                    .claim("given_name", userDetails.getGivenName())
////                    .claim("name", userDetails.getGivenName() + " " + userDetails.getFamilyName())
////                    .claim("phone_number", userDetails.getPhoneNumber())
////                    .claim("phone_number_verified", true)
////                    .claim("profile_image", userDetails.getProfileImage())
////                    .claim("picture", userDetails.getProfileImage())
////                    .claim("gender", userDetails.getGender());
////
////            var authorities = userDetails.getAuthorities().stream()
////                    .map(GrantedAuthority::getAuthority)
////                    .collect(Collectors.toList());
////            context.getClaims().claim("authorities", authorities);
////            context.getClaims().claim("roles", authorities);
////
////            log.info("ID Token customized - Claims added: uuid={}, email={}, family_name={}, given_name={}",
////                    userDetails.getUuid(), userDetails.getEmail(),
////                    userDetails.getFamilyName(), userDetails.getGivenName());
////        }
////
////        // For Access Token
////        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
////            log.info("=== Customizing Access Token ===");
////
////            context.getClaims()
////                    .claim("uuid", userDetails.getUuid())
////                    .claim("email", userDetails.getEmail())
////                    .claim("family_name", userDetails.getFamilyName())
////                    .claim("given_name", userDetails.getGivenName())
////                    .claim("phone_number", userDetails.getPhoneNumber())
////                    .claim("profile_image", userDetails.getProfileImage())
////                    .claim("gender", userDetails.getGender());
////
////            var authorities = userDetails.getAuthorities().stream()
////                    .map(GrantedAuthority::getAuthority)
////                    .collect(Collectors.toList());
////            context.getClaims().claim("authorities", authorities);
////
////            log.info("Access Token customized - UUID: {}", userDetails.getUuid());
////        }
////    }
////}
//package co.istad.itpidentityservice.security;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
//import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
//import org.springframework.stereotype.Component;
//
//import java.util.stream.Collectors;
//
//@Component
//@Slf4j
//public class CustomTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
//
//    @Override
//    public void customize(JwtEncodingContext context) {
//        Authentication principal = context.getPrincipal();
//
//        if (!(principal.getPrincipal() instanceof CustomUserDetails userDetails)) {
//            return;
//        }
//
//        // Add custom claims to ID Token
//        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
//            log.info("Adding custom claims to ID token for user: {}", userDetails.getUsername());
//
//            context.getClaims()
//                    .claim("uuid", userDetails.getUuid())
//                    .claim("email", userDetails.getEmail())
//                    .claim("family_name", userDetails.getFamilyName())
//                    .claim("given_name", userDetails.getGivenName())
//                    .claim("name", userDetails.getGivenName() + " " + userDetails.getFamilyName())
//                    .claim("phone_number", userDetails.getPhoneNumber())
//                    .claim("profile_image", userDetails.getProfileImage())
//                    .claim("gender", userDetails.getGender())
//                    .claim("authorities", userDetails.getAuthorities().stream()
//                            .map(a -> a.getAuthority())
//                            .collect(Collectors.toList()));
//        }
//    }
//}