package com.ab.authservice.oauth2;

import com.ab.authservice.jwt.JwtService;
import com.ab.authservice.model.User;
import com.ab.authservice.model.enums.AuthProvider;
import com.ab.authservice.repository.UserRepository;
import com.ab.authservice.userdetails.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    // After successful OAuth2 login, generate JWT for the local user and redirect frontend with token.
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final String FRONTEND_REDIRECT = "http://localhost:3000/oauth2/success";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Object principal = authentication.getPrincipal();

        Long userId;
        if (principal instanceof LocalOidcPrincipal p) {
            userId = p.getLocalUserId();
        } else if (principal instanceof LocalOAuth2Principal p) {
            userId = p.getLocalUserId();
        } else {
            throw new RuntimeException("Unsupported principal type: " + principal.getClass());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Local user not found after oauth login"));

        String jwt = jwtService.generateToken(new CustomUserDetails(user));

        // Redirect with token in URL fragment (#token=...), so it isn't sent to backend as a query param
        String url = FRONTEND_REDIRECT + "#token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        response.sendRedirect(url);
    }
}