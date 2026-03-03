package com.ab.authservice.api;

import com.ab.authservice.dto.auth.ForgotPasswordRequest;
import com.ab.authservice.dto.auth.ResetPasswordRequest;
import com.ab.authservice.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService resetService;


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest req,
                                    HttpServletRequest request) {
        resetService.requestReset(
                req.getEmail(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return ResponseEntity.accepted().body("If the email exists, we sent a reset link.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest req) {
        resetService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok("Password updated.");
    }
}
