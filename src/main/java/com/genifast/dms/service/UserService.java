package com.genifast.dms.service;

import com.genifast.dms.dto.request.LoginRequestDto;
import com.genifast.dms.dto.request.RefreshTokenRequestDto;
import com.genifast.dms.dto.request.ResetPasswordDto;
import com.genifast.dms.dto.request.ResetPasswordRequestDto;
import com.genifast.dms.dto.response.LoginResponseDto;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.dto.request.SignUpRequestDto;
import com.genifast.dms.dto.request.SocialLoginRequestDto;

public interface UserService {

    UserResponse getMyInfo();

    /**
     * Xử lý nghiệp vụ đăng ký người dùng mới.
     * 
     * @param signUpRequestDto DTO chứa thông tin đăng ký.
     */
    void signUp(SignUpRequestDto signUpRequestDto);

    /**
     * Xử lý nghiệp vụ đăng nhập.
     * 
     * @param loginRequestDto DTO chứa email và password.
     * @return DTO chứa access token và refresh token.
     */
    LoginResponseDto login(LoginRequestDto loginRequestDto);

    void verifyEmail(String email);

    LoginResponseDto socialLogin(SocialLoginRequestDto socialLoginDto);

    LoginResponseDto refreshToken(RefreshTokenRequestDto refreshTokenDto);

    void requestPasswordReset(ResetPasswordRequestDto resetRequestDto);

    void resetPassword(String token, ResetPasswordDto resetDto);
}