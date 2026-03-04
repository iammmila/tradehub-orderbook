package com.ab.authservice.api;

import com.ab.authservice.dto.UpdateMeRequest;
import com.ab.authservice.dto.user.ChangePasswordRequest;
import com.ab.authservice.dto.user.UserDto;
import com.ab.authservice.dto.user.UserResponse;
import com.ab.authservice.service.user.UserPasswordService;
import com.ab.authservice.service.user.UserProfileService;
import com.ab.authservice.service.user.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class UserController {
    private final UserQueryService userQueryService;
    private final UserProfileService userProfileService;
    private final UserPasswordService userPasswordService;

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> byUsername(@PathVariable String username) {
        return ResponseEntity.ok(userQueryService.getByUsername(username));
    }

    //GET /api/v1/users/me -> 200 ok
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String username = authentication.getName(); // comes from JWT subject
        return ResponseEntity.ok(userQueryService.getMe(username));
    }

    //Put /api/v1/users/me -> 200 ok
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(Authentication auth,
                                                 @Valid @RequestBody UpdateMeRequest req) {
        return ResponseEntity.ok(userProfileService.updateMe(auth.getName(), req));
    }

    //Put /api/v1/users/me/password -> 204
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication auth,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        userPasswordService.changePassword(auth.getName(), req);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/users -> 200 OK
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userQueryService.getUsers());
    }

    // GET /api/v1/users/{id} -> 200 OK or 404
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userQueryService.getUserById(userId));
    }
}
