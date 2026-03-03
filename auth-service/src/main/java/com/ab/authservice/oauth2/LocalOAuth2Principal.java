package com.ab.authservice.oauth2;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class LocalOAuth2Principal implements OAuth2User {

    private final OAuth2User delegate;
    private final Long localUserId;
    private final String localUsername;

    public LocalOAuth2Principal(OAuth2User delegate, Long localUserId, String localUsername) {
        this.delegate = delegate;
        this.localUserId = localUserId;
        this.localUsername = localUsername;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
