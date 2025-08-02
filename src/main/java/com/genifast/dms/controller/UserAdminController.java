package com.genifast.dms.controller;

import com.genifast.dms.dto.request.UserPermissionRequestDto;
import com.genifast.dms.dto.request.UserRoleUpdateRequestDto;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @PutMapping("/{userId}/roles")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequestDto requestDto) {
        UserResponse updatedUser = userService.updateUserRoles(userId, requestDto.getRoleIds());
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/{userId}/permissions")
    public ResponseEntity<String> grantPermissionToUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserPermissionRequestDto requestDto) {
        userService.grantPermission(userId, requestDto.getPermissionId());
        return ResponseEntity.ok("Permission granted successfully.");
    }

    @DeleteMapping("/{userId}/permissions/{permissionId}")
    public ResponseEntity<String> revokePermissionFromUser(
            @PathVariable Long userId,
            @PathVariable Long permissionId) {
        userService.revokePermission(userId, permissionId);
        return ResponseEntity.ok("Permission revoked successfully.");
    }
}