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
import com.genifast.dms.entity.Permission;
import com.genifast.dms.entity.Role;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.UserPermission;
import com.genifast.dms.mapper.UserMapper;
import com.genifast.dms.repository.PermissionRepository;
import com.genifast.dms.repository.RoleRepository;
import com.genifast.dms.repository.UserPermissionRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.config.ApplicationProperties;
import com.genifast.dms.config.UserDetailsCustom;
import com.genifast.dms.service.RefreshTokenService;
import com.genifast.dms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PermissionRepository permissionRepository;
        private final UserPermissionRepository userPermissionRepository;
        private final UserMapper userMapper;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtils jwtUtils;
        private final EmailService emailService;
        private final ApplicationProperties applicationProperties;
        private final RefreshTokenService refreshTokenService;
        private final UserDetailsCustom userDetailsCustom;

        @Override
        public UserResponse getMyInfo() {
                // Lấy email của user đang đăng nhập từ security context
                String currentUserEmail = JwtUtils.getCurrentUserLogin()
                                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                                                ErrorMessage.INVALID_USER.getMessage()));

                // Tìm thông tin đầy đủ của user trong DB
                User user = userRepository.findByEmail(currentUserEmail)
                                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                                                ErrorMessage.INVALID_USER.getMessage()));

                // Map từ Entity sang DTO và trả về
                return userMapper.toUserResponse(user);
        }

        @Override
        @Transactional
        public UserResponse updateUserRoles(Long userId, Set<Long> roleIds) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                                                ErrorMessage.INVALID_USER.getMessage()));

                if (user.getOrganization() == null) {
                        throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                                        "User must belong to an organization to have roles assigned.");
                }

                Set<Role> newRoles = new HashSet<>();
                if (!CollectionUtils.isEmpty(roleIds)) {
                        List<Role> foundRoles = roleRepository.findAllById(roleIds);

                        for (Role role : foundRoles) {
                                // RÀNG BUỘC QUAN TRỌNG: Role phải thuộc cùng Organization với User
                                if (role.getOrganization() == null
                                                || !role.getOrganization().getId()
                                                                .equals(user.getOrganization().getId())) {
                                        throw new ApiException(ErrorCode.INVALID_REQUEST,
                                                        "Role " + role.getName()
                                                                        + " does not belong to the user's organization.");
                                }
                                newRoles.add(role);
                        }
                }

                user.setRoles(newRoles);
                User updatedUser = userRepository.save(user);

                return userMapper.toUserResponse(updatedUser);
        }

        @Override
        @Transactional
        public void grantPermission(Long userId, Long permissionId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                                                ErrorMessage.INVALID_USER.getMessage()));

                Permission permission = permissionRepository.findById(permissionId)
                                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                                                "Permission not found."));

                // Kiểm tra xem quyền đã được gán chưa để tránh trùng lặp
                boolean alreadyExists = user.getUserPermissions().stream()
                                .anyMatch(up -> up.getPermission().getId().equals(permissionId));

                if (!alreadyExists) {
                        UserPermission userPermission = UserPermission.builder()
                                        .user(user)
                                        .permission(permission)
                                        .action("GRANT") // Có thể dùng cột action để ghi chú
                                        .build();
                        userPermissionRepository.save(userPermission);
                }
        }

        @Override
        @Transactional
        public void revokePermission(Long userId, Long permissionId) {
                // Dùng repository để xóa trực tiếp sẽ hiệu quả hơn là load entity lên rồi xóa
                userPermissionRepository.deleteByUserIdAndPermissionId(userId, permissionId);
        }

        @Override
        @Transactional
        public void signUp(SignUpRequestDto signUpRequestDto) {
                // 1. Kiểm tra email đã tồn tại chưa
                userRepository.findByEmail(signUpRequestDto.getEmail()).ifPresent(user -> {
                        throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS,
                                        ErrorMessage.EMAIL_ALREADY_EXISTS.getMessage());
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

                // 5. Gán Role "DEFAULT_USER" mặc định
                Role defaultRole = roleRepository.findByNameAndOrganizationIdIsNull("DEFAULT_USER")
                                .orElseThrow(() -> new RuntimeException("Error: Default USER role not found."));
                user.setRoles(Set.of(defaultRole));

                // 6. Lưu vào CSDL
                userRepository.save(user);

                // 7. Gửi email xác thực (logic gửi mail sẽ được thêm vào sau)
                log.info("User {} registered successfully. Sending verification email.", user.getEmail());
                emailService.sendVerifyEmailCreateAccount(user.getEmail(),
                                new VerifyEmailInfo(applicationProperties.email().linkVerifyEmail()));

        }

        @Override
        public LoginResponseDto login(LoginRequestDto loginRequestDto) {
                // 1. Tìm user theo email
                User user = userRepository.findByEmail(loginRequestDto.getEmail())
                                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_USER,
                                                ErrorMessage.INVALID_USER.getMessage()));

                // 2. Kiểm tra mật khẩu
                if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
                        throw new ApiException(ErrorCode.INVALID_USER, ErrorMessage.INVALID_USER.getMessage());
                }

                // 3. Kiểm tra trạng thái tài khoản
                if (user.getStatus() != 1) {
                        throw new ApiException(ErrorCode.USER_EMAIL_NOT_VERIFIED,
                                        ErrorMessage.USER_EMAIL_NOT_VERIFIED.getMessage());
                }

                // 4. Lấy thông tin UserDetails (bao gồm cả authorities) từ database
                UserDetails userDetails = userDetailsCustom.loadUserByUsername(user.getEmail());

                // 5. Tạo Access Token
                String accessToken = jwtUtils.generateAccessToken(user, userDetails.getAuthorities());

                // 6. Tạo/Cập nhật Refresh Token (sẽ implement chi tiết)
                String refreshToken = refreshTokenService.createRefreshToken(user);

                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user); // Cập nhật thời gian đăng nhập

                log.info("User {} logged in successfully.", user.getEmail());

                return LoginResponseDto.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build();
        }

        @Override
        public void verifyEmail(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMAIL,
                                                ErrorMessage.INVALID_EMAIL.getMessage()));

                user.setStatus(1); // 1 = Active
                userRepository.save(user);
                log.info("Email {} verified successfully.", email);
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

                // Lấy thông tin UserDetails (bao gồm cả authorities) từ database
                UserDetails userDetails = userDetailsCustom.loadUserByUsername(user.getEmail());

                String accessToken = jwtUtils.generateAccessToken(user, userDetails.getAuthorities());
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

                // Lấy thông tin UserDetails (bao gồm cả authorities) từ database
                UserDetails userDetails = userDetailsCustom.loadUserByUsername(user.getEmail());

                // Tạo access token mới
                String newAccessToken = jwtUtils.generateAccessToken(user, userDetails.getAuthorities());

                // Tạo refresh token mới (xoay vòng token để tăng bảo mật)
                String newRefreshToken = refreshTokenService.createRefreshToken(user);

                return LoginResponseDto.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .build();
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