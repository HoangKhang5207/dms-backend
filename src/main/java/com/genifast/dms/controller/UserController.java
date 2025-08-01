package com.genifast.dms.controller;

import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('DEFAULT_USER') and hasAuthority('user:view-profile')")
    public ResponseEntity<UserResponse> getMyInfo() {
        UserResponse currentUserInfo = userService.getMyInfo();
        return ResponseEntity.ok(currentUserInfo);
    }
}