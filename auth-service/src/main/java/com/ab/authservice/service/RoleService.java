package com.ab.authservice.service;

import com.ab.authservice.dto.role.AssignRoleRequest;
import com.ab.authservice.dto.role.CreateRoleRequest;
import com.ab.authservice.dto.role.RoleResponse;
import com.ab.authservice.model.Role;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.RoleRepository;
import com.ab.authservice.repository.UserRepository;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // Returns all roles as lightweight DTOs.
    public List<RoleResponse> getAllRoles() {

        return roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }

    // Creates a new role after normalizing name to ROLE_XXX.
    public void createRole(CreateRoleRequest request) {
        String formatted = normalizeRoleName(request.getName()); // ROLE_ADMIN etc.

        if (roleRepository.findByName(formatted).isPresent()) {
            throw new BadRequestException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        roleRepository.save(Role.builder()
                .name(formatted)
                .build());
    }

    // Deletes role by id (consider checking existence + preventing delete if used).
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }

    // Assigns a role to a user by username (admin action).
    public void assignRoleToUser(AssignRoleRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        String formattedRole = normalizeRoleName(request.getRoleName());

        Role role = roleRepository.findByName(formattedRole)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_NOT_FOUND));

        user.setRole(role);
        userRepository.save(user);
    }

    // Ensures role naming convention: ROLE_ADMIN / ROLE_USER etc.
    private String normalizeRoleName(String raw) {
        String name = raw.trim().toUpperCase();
        return name.startsWith("ROLE_") ? name : "ROLE_" + name;
    }
}
