package com.genifast.dms.controller;

import com.genifast.dms.dto.response.PermissionResponseDto;
import com.genifast.dms.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class PermissionAdminController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionResponseDto>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }
}