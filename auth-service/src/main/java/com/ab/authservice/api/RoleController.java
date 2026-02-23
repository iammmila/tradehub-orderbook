package com.ab.authservice.api;

import com.ab.authservice.dto.role.AssignRoleRequest;
import com.ab.authservice.dto.role.CreateRoleRequest;
import com.ab.authservice.dto.role.RoleResponse;
import com.ab.authservice.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    //get /api/v1/admin/roles -> 200 ok
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    //post /api/v1/admin/roles -> 201 created
    @PostMapping("/roles")
    public ResponseEntity<Void> createRole(@Valid @RequestBody CreateRoleRequest request) {
        roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //delete /api/v1/admin/roles/{roleId} -> 204 no content
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    //put /api/v1/admin/roles/assign -> 200 ok
    @PutMapping("/roles/assign")
    public ResponseEntity<Void> assignRoleToUser(
            @Valid @RequestBody AssignRoleRequest request) {

        roleService.assignRoleToUser(request);
        return ResponseEntity.ok().build();
    }
}
