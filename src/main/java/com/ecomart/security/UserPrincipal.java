package com.ecomart.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private String role;
    private boolean active;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(Long id, String email, String password, String role, boolean active) {
        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        return UserPrincipal.builder()
                .id(id)
                .email(email)
                .password(password)
                .role(role)
                .active(active)
                .authorities(Collections.singletonList(authority))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
