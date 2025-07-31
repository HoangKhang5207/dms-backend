package com.genifast.dms.common.utils;

import com.genifast.dms.config.ApplicationProperties;
import com.genifast.dms.entity.User;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class JwtUtils {

    private final JwtEncoder accessTokenEncoder;
    private final JwtEncoder passwordResetTokenEncoder;
    private final JwtDecoder passwordResetTokenDecoder;
    private final ApplicationProperties applicationProperties;

    public JwtUtils(@Qualifier("accessTokenEncoder") JwtEncoder accessTokenEncoder,
            @Qualifier("passwordResetTokenEncoder") JwtEncoder passwordResetTokenEncoder,
            @Qualifier("passwordResetTokenDecoder") JwtDecoder passwordResetTokenDecoder,
            ApplicationProperties applicationProperties) {
        this.accessTokenEncoder = accessTokenEncoder;
        this.passwordResetTokenEncoder = passwordResetTokenEncoder;
        this.passwordResetTokenDecoder = passwordResetTokenDecoder;
        this.applicationProperties = applicationProperties;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("GeniFast-Search-Java")
                .issuedAt(now)
                .expiresAt(now.plus(applicationProperties.jwt().accessTokenDuration()))
                .subject(user.getEmail())
                .claim("user_id", user.getId())
                .claim("name", user.getLastName() + " " + user.getFirstName())
                .claim("email", user.getEmail())
                .claim("is_admin", user.getIsAdmin())
                .claim("aud", "user_credentials")
                .build();

        return this.accessTokenEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generatePasswordResetToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("GeniFast-PasswordReset")
                .issuedAt(now)
                .expiresAt(now.plus(applicationProperties.resetPassword().tokenTtl()))
                .subject(user.getEmail())
                .claim("user_id", user.getId())
                .claim("name", user.getFirstName() + " " + user.getLastName())
                .claim("email", user.getEmail())
                .claim("is_admin", user.getIsAdmin())
                .claim("aud", "reset_password")
                .build();
        return this.passwordResetTokenEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String getEmailFromPasswordResetToken(String token) {
        Jwt jwt = this.passwordResetTokenDecoder.decode(token);
        return jwt.getSubject();
    }

    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            // Luồng cũ (nếu còn sử dụng): Principal là một đối tượng UserDetails.
            return ((UserDetails) principal).getUsername();
        }

        if (principal instanceof Jwt) {
            // Luồng Keycloak MỚI: Principal là một đối tượng Jwt.
            // Ưu tiên lấy claim 'email' để đảm bảo tính nhất quán.
            return ((Jwt) principal).getSubject();
        }

        if (principal instanceof String) {
            // Fallback nếu principal là một chuỗi.
            return (String) principal;
        }

        return null;
    }
}