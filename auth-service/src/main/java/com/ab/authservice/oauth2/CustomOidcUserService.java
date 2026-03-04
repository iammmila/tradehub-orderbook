package com.ab.authservice.oauth2;

import com.ab.authservice.model.Role;
import com.ab.authservice.model.User;
import com.ab.authservice.model.enums.AuthProvider;
import com.ab.authservice.repository.RoleRepository;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    // Creates/links a local User record when someone logs in with Google (OIDC).
    // Returns a principal that contains both Google user info + local user id.
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        AuthProvider provider = AuthProvider.GOOGLE;
        String providerId = oidcUser.getSubject(); // same as "sub"
        String email = oidcUser.getEmail();

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email not found from OIDC provider");
        }

        // Try to find existing user by (provider, providerId), fallback to email match
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        // First-time Google login => create local user
        if (user == null) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

            String firstName = oidcUser.getGivenName() != null ? oidcUser.getGivenName() : "OAuth";
            String lastName = oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : "User";

            user = User.builder()
                    .username(email.split("@")[0]) // consider making unique if collision happens
                    .email(email.toLowerCase())
                    .firstName(firstName)
                    .lastName(lastName)
                    .password(null) // OAuth users don't have local password by default
                    .role(userRole)
                    .provider(provider)
                    .providerId(providerId)
                    .build();

            user = userRepository.save(user);
        }

        return new LocalOidcPrincipal(oidcUser, user.getId(), user.getUsername());
    }
}
