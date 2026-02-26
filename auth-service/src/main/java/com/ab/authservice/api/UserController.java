package com.ab.authservice.api;

import com.ab.authservice.dto.UpdateMeRequest;
import com.ab.authservice.dto.user.ChangePasswordRequest;
import com.ab.authservice.dto.user.UserDto;
import com.ab.authservice.dto.user.UserResponse;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.UserRepository;
import com.ab.authservice.service.UserService;
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
    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> byUsername(@PathVariable String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(new UserDto(u.getId(), u.getUsername()));
    }

    //GET /api/v1/users/me -> 200 ok
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String username = authentication.getName(); // comes from JWT subject
        return ResponseEntity.ok(userService.getMe(username));
    }

    //Put /api/v1/users/me -> 200 ok
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(Authentication auth,
                                                 @Valid @RequestBody UpdateMeRequest req) {
        return ResponseEntity.ok(userService.updateMe(auth.getName(), req));
    }

    //Put /api/v1/users/me/password -> 204
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication auth,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(auth.getName(), req);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/users -> 200 OK
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    // GET /api/v1/users/{id} -> 200 OK or 404
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
