package com.genifast.dms.controller;

import com.genifast.dms.dto.request.LoginRequestDto;
import com.genifast.dms.dto.request.RefreshTokenRequestDto;
import com.genifast.dms.dto.request.ResetPasswordRequestDto;
import com.genifast.dms.dto.response.LoginResponseDto;
import com.genifast.dms.dto.request.SignUpRequestDto;
import com.genifast.dms.dto.request.SocialLoginRequestDto;
import com.genifast.dms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    // private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        userService.signUp(signUpRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully. Please check your email to verify your account.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        // Nạp input gồm username và password vào Security
        // UsernamePasswordAuthenticationToken authenticationToken = new
        // UsernamePasswordAuthenticationToken(
        // loginRequestDto.getEmail(), loginRequestDto.getPassword());

        // // xác thực người dùng => cần viết hàm loadUserByUsername
        // Authentication authentication =
        // authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // // set thông tin người dùng đăng nhập vào SecurityContext
        // SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginResponseDto response = userService.login(loginRequestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("email") String email) {
        userService.verifyEmail(email);
        return ResponseEntity.ok("Email verified successfully.");
    }

    @PostMapping("/social-login")
    public ResponseEntity<LoginResponseDto> socialLogin(@Valid @RequestBody SocialLoginRequestDto socialLoginDto) {
        return ResponseEntity.ok(userService.socialLogin(socialLoginDto));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequestDto refreshTokenDto) {
        return ResponseEntity.ok(userService.refreshToken(refreshTokenDto));
    }

    @PostMapping("/reset-password/request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody ResetPasswordRequestDto resetRequestDto) {
        userService.requestPasswordReset(resetRequestDto);
        return ResponseEntity.ok("Password reset link has been sent to your email.");
    }
}
