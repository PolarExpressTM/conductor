package org.eu.polarexpress.conductor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class Conductor extends BaseEntity implements UserDetails, OAuth2User, OidcUser {
    @NotBlank
    @Setter
    private String snowflakeId;
    @NotBlank
    @Setter
    private String username;
    @NotBlank
    @Setter
    private String globalName;
    @NotBlank
    @Setter
    private String locale;
    @Email
    @Setter
    private String email;
    @Pattern(regexp = "(|^(\\s*|.{8,})$)")
    @Setter
    private String password;
    @Enumerated(EnumType.STRING)
    @Setter
    @Builder.Default
    protected Role role = Role.NORMAL;
    @Setter
    @Builder.Default
    protected boolean locked = false;
    @Setter
    @Builder.Default
    protected boolean enabled = true;
    @Transient
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    @Transient
    @Builder.Default
    private Map<String, Object> claims = new HashMap<>();

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, Object> getClaims() {
        return claims;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return OidcUserInfo.builder()
                .name(username)
                .email(email)
                .build();
    }

    @Override
    public OidcIdToken getIdToken() {
        return OidcIdToken.withTokenValue(username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(168, ChronoUnit.HOURS))
                .build();
    }

    @Override
    public String getName() {
        return username;
    }
}
