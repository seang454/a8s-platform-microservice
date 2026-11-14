package co.istad.iam_service.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
@Setter
public class CustomUserDetails extends User {

    private String uuid;
    private String email;
    private String familyName;
    private String givenName;
    private String phoneNumber;
    private String profileImage;
    private String gender;


    public CustomUserDetails(String username,
                             String password,
                             boolean enabled,
                             boolean accountNonExpired,
                             boolean credentialsNonExpired,
                             boolean accountNonLocked,
                             Collection<? extends GrantedAuthority> authorities,
                             String uuid,
                             String email,
                             String familyName,
                             String givenName,
                             String phoneNumber,
                             String profileImage,
                             String gender) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.uuid = uuid;
        this.email = email;
        this.familyName = familyName;
        this.givenName = givenName;
        this.phoneNumber = phoneNumber;
        this.profileImage = profileImage;
        this.gender = gender;
    }
}

//package co.istad.itpidentityservice.security;
//
//import co.istad.itpidentityservice.domain.User;
//import lombok.Getter;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//@Getter
//public class CustomUserDetails implements UserDetails {
//
//    private final String uuid;
//    private final String username;
//    private final String password;
//    private final String email;
//    private final String familyName;
//    private final String givenName;
//    private final String phoneNumber;
//    private final LocalDate dob;
//    private final String gender;
//    private final String profileImage;
//    private final String coverImage;
//    private final boolean accountNonExpired;
//    private final boolean accountNonLocked;
//    private final boolean credentialsNonExpired;
//    private final boolean enabled;
//    private final Collection<? extends GrantedAuthority> authorities;
//
//    public CustomUserDetails(User user) {
//        this.uuid = user.getUuid();
//        this.username = user.getUsername();
//        this.password = user.getPassword();
//        this.email = user.getEmail();
//        this.familyName = user.getFamilyName();
//        this.givenName = user.getGivenName();
//        this.phoneNumber = user.getPhoneNumber();
//        this.dob = user.getDob();
//        this.gender = user.getGender();
//        this.profileImage = user.getProfileImage();
//        this.coverImage = user.getCoverImage();
//        this.accountNonExpired = user.getAccountNonExpired();
//        this.accountNonLocked = user.getAccountNonLocked();
//        this.credentialsNonExpired = user.getCredentialsNonExpired();
//        this.enabled = user.getIsEnabled();
//        this.authorities = buildAuthorities(user);
//    }
//
//    private Collection<? extends GrantedAuthority> buildAuthorities(User user) {
//        List<GrantedAuthority> authorities = new ArrayList<>();
//
//        user.getRoles().forEach(role -> {
//            // Add role authority
//            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
//
//            // Add permission authorities
//            role.getPermissions().forEach(permission -> {
//                authorities.add(new SimpleGrantedAuthority(permission.getName()));
//            });
//        });
//
//        return authorities;
//    }
//
//    public String getFullName() {
//        return givenName + " " + familyName;
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return authorities;
//    }
//
//    @Override
//    public String getPassword() {
//        return password;
//    }
//
//    @Override
//    public String getUsername() {
//        return username;
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return accountNonExpired;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return accountNonLocked;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return credentialsNonExpired;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return enabled;
//    }
//}