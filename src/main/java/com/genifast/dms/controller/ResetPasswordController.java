package com.genifast.dms.controller;

import com.genifast.dms.dto.request.ResetPasswordDto;
import com.genifast.dms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reset-password")
@RequiredArgsConstructor
public class ResetPasswordController {

    private final UserService userService;

    // Endpoint này yêu cầu có token reset password hợp lệ trong header
    @PostMapping
    public ResponseEntity<String> resetPassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ResetPasswordDto resetDto) {
        // Lấy token từ header (bỏ "Bearer ")
        String token = authorizationHeader.substring(7);
        userService.resetPassword(token, resetDto);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}
