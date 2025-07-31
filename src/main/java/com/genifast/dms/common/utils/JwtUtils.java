package com.genifast.dms.common.utils;

import com.genifast.dms.config.ApplicationProperties;
import com.genifast.dms.dto.response.LoginResponseDto;
import com.genifast.dms.entity.User;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Optional;

@Service
public class JwtUtils {

    // --- Giữ lại các dependencies cũ cho các luồng token nội bộ ---
    // private final JwtEncoder accessTokenEncoder;
    private final JwtEncoder passwordResetTokenEncoder;
    private final JwtDecoder passwordResetTokenDecoder;
    private final ApplicationProperties applicationProperties;

    // --- Bổ sung dependencies mới cho việc tích hợp Keycloak ---
    private final Keycloak keycloakAdminClient;
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.resource}")
    private String clientId;
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public JwtUtils(// @Qualifier("accessTokenEncoder") JwtEncoder accessTokenEncoder,
            @Qualifier("passwordResetTokenEncoder") JwtEncoder passwordResetTokenEncoder,
            @Qualifier("passwordResetTokenDecoder") JwtDecoder passwordResetTokenDecoder,
            ApplicationProperties applicationProperties,
            Keycloak keycloakAdminClient) { // Inject Keycloak bean
        // this.accessTokenEncoder = accessTokenEncoder;
        this.passwordResetTokenEncoder = passwordResetTokenEncoder;
        this.passwordResetTokenDecoder = passwordResetTokenDecoder;
        this.applicationProperties = applicationProperties;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    // --- Các phương thức Keycloak Admin Client ---

    /**
     * Tạo một user mới trên Keycloak. User được tạo ở trạng thái "chưa kích hoạt".
     * 
     * @param user     Đối tượng User chứa thông tin cần tạo.
     * @param password Mật khẩu thô của người dùng.
     * @return ID của user được tạo trên Keycloak.
     * @throws IllegalStateException nếu tạo user thất bại.
     */
    public String createUserInKeycloak(User user, String password) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getEmail());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setFirstName(user.getFirstName());
        userRepresentation.setLastName(user.getLastName());
        userRepresentation.setEnabled(false); // Quan trọng: User chưa được kích hoạt
        userRepresentation.setEmailVerified(false);

        Response response = keycloakAdminClient.realm(realm).users().create(userRepresentation);

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String error = response.readEntity(String.class);
            throw new IllegalStateException("Could not create user in Keycloak: " + error);
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);

        keycloakAdminClient.realm(realm).users().get(userId).resetPassword(passwordCred);
        return userId;
    }

    /**
     * Kích hoạt một user trên Keycloak.
     * 
     * @param email Email của user cần kích hoạt.
     */
    public void enableUserInKeycloak(String email) {
        keycloakAdminClient.realm(realm).users().searchByEmail(email, true).stream()
                .findFirst()
                .ifPresent(userRep -> {
                    userRep.setEnabled(true);
                    userRep.setEmailVerified(true);
                    keycloakAdminClient.realm(realm).users().get(userRep.getId()).update(userRep);
                });
    }

    /**
     * Lấy Access Token và Refresh Token từ Keycloak bằng username và password.
     * 
     * @param email    Email của người dùng.
     * @param password Mật khẩu thô của người dùng.
     * @return LoginResponseDto chứa các token.
     */
    public String getTokensFromKeycloak(String email, String password) {
        RestTemplate restTemplate = new RestTemplate();
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "password");
        map.add("username", email);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(tokenUrl, request,
                LoginResponseDto.class);
        return response.getBody().getAccessToken();
    }

    public String generateAccessToken(User user) {
        // Phương thức này vẫn giữ nguyên logic tạo token nội bộ.
        // Nó có thể được dùng cho các mục đích khác ngoài luồng login chính.
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

        // return
        // this.accessTokenEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return "";
    }

    public String generatePasswordResetToken(User user) {
        // Giữ nguyên hoàn toàn
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
        // Giữ nguyên hoàn toàn
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
            return ((Jwt) principal).getClaimAsString("email");
        }

        if (principal instanceof String) {
            // Fallback nếu principal là một chuỗi.
            return (String) principal;
        }

        return null;
    }
}