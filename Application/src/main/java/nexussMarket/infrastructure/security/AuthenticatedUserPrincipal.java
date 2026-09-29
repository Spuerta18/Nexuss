package nexussMarket.infrastructure.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Adapts a domain {@link User} to Spring Security's {@link UserDetails}.
 * Controllers receive it with {@code @AuthenticationPrincipal} and read the
 * real domain user through {@link #getUser()}.
 *
 * <p>The single authority is {@code ROLE_<SystemRole code>} (e.g.
 * {@code ROLE_SELLER}), so routes and methods can be restricted with
 * {@code hasRole("SELLER")}. Only {@code ACTIVE} users are enabled;
 * {@code BLOCKED} users are also reported as locked.</p>
 */
public class AuthenticatedUserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final transient User user;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUserPrincipal(User user) {
        this.user = user;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode()));
    }

    /** The authenticated domain user. */
    public User getUser() {
        return user;
    }

    /** Identifier of the authenticated user. */
    public String getUserId() {
        return user.getIdentifier();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.BLOCKED;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
