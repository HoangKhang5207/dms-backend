package com.genifast.dms.service.impl;

import com.genifast.dms.dto.request.LoginRequestDto;
import com.genifast.dms.dto.request.RefreshTokenRequestDto;
import com.genifast.dms.dto.request.ResetPasswordDto;
import com.genifast.dms.dto.request.ResetPasswordRequestDto;
import com.genifast.dms.dto.response.LoginResponseDto;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.dto.request.SignUpRequestDto;
import com.genifast.dms.dto.request.SocialLoginRequestDto;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.dto.ResetPasswordInfo;
import com.genifast.dms.common.dto.VerifyEmailInfo;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.service.EmailService;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.UserMapper;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.config.ApplicationProperties;
import com.genifast.dms.service.RefreshTokenService;
import com.genifast.dms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final ApplicationProperties applicationProperties;
    private final RefreshTokenService refreshTokenService;

    private String passwordForRefreshToken = "";

    @Override
    public UserResponse getMyInfo() {
        // Lấy email của user đang đăng nhập từ security context
        String currentUserEmail = JwtUtils.getCurrentUserLogin()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));

        // Tìm thông tin đầy đủ của user trong DB
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));

        // Map từ Entity sang DTO và trả về
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {
        // 1. Kiểm tra email đã tồn tại chưa
        userRepository.findByEmail(signUpRequestDto.getEmail()).ifPresent(user -> {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, ErrorMessage.EMAIL_ALREADY_EXISTS.getMessage());
        });

        // 2. Map DTO sang Entity
        User user = userMapper.toUser(signUpRequestDto);

        // 3. Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));

        // 4. Thiết lập trạng thái ban đầu (chưa xác thực)
        user.setStatus(2); // Tương ứng logic Golang

        user.setIsAdmin(false);
        user.setIsOrganizationManager(false);
        user.setIsDeptManager(false);

        // 5. Lưu vào CSDL
        userRepository.save(user);

        // 6. Tạo user trên Keycloak (ở trạng thái disabled)
        try {
            jwtUtils.createUserInKeycloak(user, signUpRequestDto.getPassword());
            log.info("User {} created in Keycloak (disabled).", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak during signup.", e);
            // Quan trọng: Ném ra một exception để transaction được rollback,
            // tránh trường hợp user tồn tại ở DB cục bộ mà không có trên Keycloak.
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to provision user to identity provider.");
        }

        // 7. Gửi email xác thực (giữ nguyên logic nghiệp vụ)
        log.info("User {} registered successfully. Sending verification email.", user.getEmail());
        emailService.sendVerifyEmailCreateAccount(user.getEmail(),
                new VerifyEmailInfo(applicationProperties.email().linkVerifyEmail()));
    }

    @Override
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_USER, ErrorMessage.INVALID_USER.getMessage()));

        // 2. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_USER, ErrorMessage.INVALID_USER.getMessage());
        }
        // Lưu mật khẩu để sử dụng trong việc tạo Refresh Token
        this.passwordForRefreshToken = loginRequestDto.getPassword();

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() != 1) {
            throw new ApiException(ErrorCode.USER_EMAIL_NOT_VERIFIED,
                    ErrorMessage.USER_EMAIL_NOT_VERIFIED.getMessage());
        }

        // 4. Tạo Access Token
        // String accessToken = jwtUtils.generateAccessToken(user);

        // 5. Tạo/Cập nhật Refresh Token (sẽ implement chi tiết)
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User {} logged in successfully.", user.getEmail());

        try {
            log.info("User {} authenticated locally. Fetching tokens from Keycloak...", user.getEmail());

            // 6. Tạo Access Token
            String accessToken = jwtUtils.getTokensFromKeycloak(loginRequestDto.getEmail(),
                    loginRequestDto.getPassword());

            return LoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (Exception e) {
            log.error("Keycloak token retrieval failed for user {}: {}", loginRequestDto.getEmail(), e.getMessage());
            // Lỗi này thường xảy ra nếu user/pass đúng ở DB cục bộ nhưng sai trên Keycloak
            // (do mất đồng bộ), hoặc Keycloak bị lỗi.
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Authentication with identity provider failed.");
        }

        // return LoginResponseDto.builder()
        // .accessToken(accessToken)
        // .refreshToken(refreshToken)
        // .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMAIL,
                        ErrorMessage.INVALID_EMAIL.getMessage()));

        // 1. Cập nhật trạng thái trong DB cục bộ
        user.setStatus(1); // 1 = Active
        userRepository.save(user);
        log.info("Email {} verified successfully in local DB.", email);

        // 2. Kích hoạt user tương ứng trên Keycloak
        try {
            jwtUtils.enableUserInKeycloak(email);
            log.info("User {} enabled successfully in Keycloak.", email);
        } catch (Exception e) {
            log.error("Failed to enable user {} in Keycloak.", email, e);
            // Ném exception để rollback transaction
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to activate user in identity provider.");
        }
    }

    @Override
    @Transactional
    public LoginResponseDto socialLogin(SocialLoginRequestDto socialLoginDto) {
        // Tìm user, nếu không có thì tạo mới
        User user = userRepository.findByEmail(socialLoginDto.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .firstName(socialLoginDto.getFirstName())
                            .lastName(socialLoginDto.getLastName())
                            .email(socialLoginDto.getEmail())
                            .gender(false)
                            .isAdmin(false)
                            .isOrganizationManager(false)
                            .isSocial(true)
                            .status(1) // Kích hoạt ngay
                            .build();
                    return userRepository.save(newUser);
                });

        // Nếu user tồn tại nhưng chưa được đánh dấu là social, cập nhật lại
        if (!user.getIsSocial()) {
            user.setIsSocial(true);
            userRepository.save(user);
        }

        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public LoginResponseDto refreshToken(RefreshTokenRequestDto refreshTokenDto) {
        // Lấy thông tin user từ refresh token
        User user = refreshTokenService.verifyAndGetUser(refreshTokenDto.getRefreshToken());

        // Tạo access token mới
        // String newAccessToken = jwtUtils.generateAccessToken(user);

        // Tạo refresh token mới (xoay vòng token để tăng bảo mật)
        String refreshToken = refreshTokenService.createRefreshToken(user);

        try {
            // Tạo Access Token
            String accessToken = jwtUtils.getTokensFromKeycloak(user.getEmail(),
                    passwordForRefreshToken);

            return LoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (Exception e) {
            log.error("Keycloak token retrieval failed for user {}: {}", user.getEmail(), e.getMessage());
            // Lỗi này thường xảy ra nếu user/pass đúng ở DB cục bộ nhưng sai trên Keycloak
            // (do mất đồng bộ), hoặc Keycloak bị lỗi.
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Authentication with identity provider failed.");
        }
    }

    @Override
    public void requestPasswordReset(ResetPasswordRequestDto resetRequestDto) {
        User user = userRepository.findByEmail(resetRequestDto.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_USER,
                        ErrorMessage.INVALID_USER.getMessage()));

        // Tạo token reset password (logic sẽ nằm trong JwtService)
        String resetToken = jwtUtils.generatePasswordResetToken(user);

        // Gửi email chứa link reset (logic gửi mail sẽ được gọi ở đây)
        log.info("Password reset link sent for user {}", user.getEmail());
        emailService.sendResetPasswordLink(user.getEmail(),
                new ResetPasswordInfo(applicationProperties.email().linkResetPassword(), resetToken));
    }

    @Override
    @Transactional
    public void resetPassword(String token, ResetPasswordDto resetDto) {
        // Tầng Security đã validate token, ở đây ta chỉ cần lấy thông tin
        // Giả sử JwtService có hàm để lấy email từ token
        String email = jwtUtils.getEmailFromPasswordResetToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMAIL,
                        ErrorMessage.INVALID_EMAIL.getMessage()));

        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userRepository.save(user);
        log.info("Password has been reset successfully for user {}", email);
    }
}