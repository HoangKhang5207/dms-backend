package com.genifast.dms.config;

import com.genifast.dms.common.utils.PemUtils;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final Resource accessTokenPublicResource;
    private final Resource accessTokenPrivateResource;
    private final Resource resetPassPublicResource;
    private final Resource resetPassPrivateResource;

    public SecurityConfig(ApplicationProperties applicationProperties) {
        this.accessTokenPublicResource = applicationProperties.jwt().accessTokenPublicKey();
        this.accessTokenPrivateResource = applicationProperties.jwt().accessTokenPrivateKey();
        this.resetPassPublicResource = applicationProperties.resetPassword().publicKey();
        this.resetPassPrivateResource = applicationProperties.resetPassword().privateKey();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1) // Ưu tiên cao hơn
    public SecurityFilterChain resetPasswordSecurityFilterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {
        return http
                .securityMatcher("/api/v1/reset-password") // Chỉ áp dụng cho endpoint này
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/reset-password").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(passwordResetTokenDecoder()))
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {

        String[] whiteList = {
                "/", "/api/v1/auth/login", "/api/v1/auth/refresh-token", "/api/v1/auth/signup",
                "/storage/**", "/api/v1/auth/email/**", "/v3/api-docs/**", "/swagger-ui.html",
                "/swagger-ui/**", "/api/v1/auth/reset-password/request", "/api/v1/invitation/**",
                "/api/v1/search-history/**"
        };

        return http
                .securityMatcher("/api/v1/**")
                // Vô hiệu hóa CSRF vì dùng API stateless
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Cấu hình quyền truy cập cho các endpoint
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(whiteList).permitAll()
                        .anyRequest().authenticated())
                // Cấu hình JWT-based security
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(accessTokenDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                // Không tạo session, mỗi request phải tự xác thực
                .formLogin(l -> l.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    // Bean mới để chuyển đổi claims trong JWT thành Roles
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Cấu hình để nó không đọc claim "scope" mặc định và thêm tiền tố "SCOPE_"
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope"); // Đọc từ claim "scope"

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    // @Bean
    // @Primary // Đánh dấu đây là JwtDecoder mặc định
    // public JwtDecoder keycloakJwtDecoder(
    // @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String
    // jwkSetUri) {
    // return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    // }

    @Bean
    @Qualifier("accessTokenDecoder")
    public JwtDecoder accessTokenDecoder() {
        return NimbusJwtDecoder.withPublicKey(accessTokenPublicKey()).build();
    }

    @Bean
    @Qualifier("accessTokenEncoder")
    public JwtEncoder accessTokenEncoder() {
        JWK jwk = new RSAKey.Builder(accessTokenPublicKey()).privateKey(accessTokenPrivateKey()).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    private RSAPublicKey accessTokenPublicKey() {
        try {
            String pem = StreamUtils.copyToString(accessTokenPublicResource.getInputStream(),
                    StandardCharsets.UTF_8);
            byte[] der = PemUtils.parseDerFromPem(pem,
                    "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read Access Token public key from " + accessTokenPublicResource, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to parse Access Token public key", e);
        }
    }

    private RSAPrivateKey accessTokenPrivateKey() {
        try {
            String pem = StreamUtils.copyToString(accessTokenPrivateResource.getInputStream(),
                    StandardCharsets.UTF_8);
            byte[] der = PemUtils.parseDerFromPem(pem,
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read Access Token private key from " + accessTokenPrivateResource,
                    e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to parse Access Token private key", e);
        }
    }

    @Bean
    @Qualifier("passwordResetTokenDecoder")
    public JwtDecoder passwordResetTokenDecoder() {
        return NimbusJwtDecoder.withPublicKey(passwordResetPublicKey()).build();
    }

    @Bean
    @Qualifier("passwordResetTokenEncoder")
    public JwtEncoder passwordResetTokenEncoder() {
        JWK jwk = new RSAKey.Builder(passwordResetPublicKey()).privateKey(passwordResetPrivateKey()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    private RSAPublicKey passwordResetPublicKey() {
        try {
            String pem = StreamUtils.copyToString(resetPassPublicResource.getInputStream(), StandardCharsets.UTF_8);
            byte[] der = PemUtils.parseDerFromPem(pem,
                    "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read Reset Password Token public key from " + resetPassPublicResource, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to parse Reset Password Token public key", e);
        }
    }

    private RSAPrivateKey passwordResetPrivateKey() {
        try {
            String pem = StreamUtils.copyToString(resetPassPrivateResource.getInputStream(), StandardCharsets.UTF_8);
            byte[] der = PemUtils.parseDerFromPem(pem,
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read Reset Password Token private key from " + resetPassPrivateResource, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to parse Reset Password Token private key", e);
        }
    }
}