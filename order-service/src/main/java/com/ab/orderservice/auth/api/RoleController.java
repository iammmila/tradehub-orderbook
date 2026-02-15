package com.ab.orderservice.auth.api;

import com.ab.orderservice.auth.dto.role.AssignRoleRequest;
import com.ab.orderservice.auth.dto.role.CreateRoleRequest;
import com.ab.orderservice.auth.dto.role.RoleResponse;
import com.ab.orderservice.auth.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PostMapping("/roles")
    public ResponseEntity<Void> createRole(@Valid @RequestBody CreateRoleRequest request) {
        roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/roles/assign")
    public ResponseEntity<Void> assignRoleToUser(
            @Valid @RequestBody AssignRoleRequest request) {

        roleService.assignRoleToUser(request);
        return ResponseEntity.ok().build();
    }
}
