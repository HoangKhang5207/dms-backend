package com.genifast.dms.controller;

import com.genifast.dms.dto.request.RoleRequestDto;
import com.genifast.dms.dto.response.RoleResponseDto;
import com.genifast.dms.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Quản trị viên')")
public class RoleAdminController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleResponseDto> createRole(@Valid @RequestBody RoleRequestDto roleDto) {
        return new ResponseEntity<>(roleService.createRole(roleDto), HttpStatus.CREATED);
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<List<RoleResponseDto>> getRolesByOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(roleService.getAllRolesByOrg(orgId));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> updateRole(@PathVariable Long roleId,
            @Valid @RequestBody RoleRequestDto roleDto) {
        return ResponseEntity.ok(roleService.updateRole(roleId, roleDto));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> getRoleById(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }
}