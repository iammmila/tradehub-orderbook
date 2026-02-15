package com.ab.orderservice.auth.service;

import com.ab.orderservice.auth.dto.role.AssignRoleRequest;
import com.ab.orderservice.auth.dto.role.CreateRoleRequest;
import com.ab.orderservice.auth.dto.role.RoleResponse;
import com.ab.orderservice.auth.model.Role;
import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.auth.repository.RoleRepository;
import com.ab.orderservice.auth.repository.UserRepository;
import com.ab.orderservice.common.exception.ErrorCode;
import com.ab.orderservice.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<RoleResponse> getAllRoles() {

        return roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }

    public void createRole(CreateRoleRequest request) {
        String formatted = normalizeRoleName(request.getName()); // ROLE_ADMIN etc.

        if (roleRepository.findByName(formatted).isPresent()) {
            throw new RuntimeException("Role already exists");
        }

        roleRepository.save(Role.builder()
                .name(formatted)
                .build());
    }

    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }

    public void assignRoleToUser(AssignRoleRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        String formattedRole = normalizeRoleName(request.getRoleName());

        Role role = roleRepository.findByName(formattedRole)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRole(role);
        userRepository.save(user);
    }

    private String normalizeRoleName(String raw) {
        String name = raw.trim().toUpperCase();
        return name.startsWith("ROLE_") ? name : "ROLE_" + name;
    }
}
