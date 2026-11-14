
package com.Vishleshan.Api.security;

import com.Vishleshan.Api.entity.AccountType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AppUserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final AccountType accountType;
    private final List<GrantedAuthority> authorities;
    private final boolean enabled;

    public AppUserPrincipal(
            UUID id,
            String email,
            String password,
            AccountType accountType,
            String roleName,
            boolean enabled) {

        this.id = id;
        this.email = email;
        this.password = password;
        this.accountType = accountType;
        this.enabled = enabled;

        this.authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + roleName)
        );
    }

    public UUID getId() {
        return id;
    }

    public AccountType getAccountType() {
        return accountType;
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
