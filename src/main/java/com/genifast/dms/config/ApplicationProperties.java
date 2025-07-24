// src/main/java/com/genifast/dms/config/ApplicationProperties.java
package com.genifast.dms.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "application")
@Validated // Bật tính năng validation cho lớp này
public record ApplicationProperties(
                @NotNull @Valid AppInfoProperties appInfo,
                @NotNull @Valid KafkaProperties kafka,
                @NotNull @Valid JwtProperties jwt,
                @NotNull @Valid EmailProperties email,
                @NotNull @Valid ResetPasswordProperties resetPassword) {

        // 2. Lớp con cho App Info
        public record AppInfoProperties(
                        @NotBlank String host,
                        @NotBlank String name) {
        }

        // 3. Lớp con cho Kafka
        public record KafkaProperties(
                        @NotBlank String brokers,
                        @NotBlank String groupId,
                        String topicTest,
                        boolean enableTls,
                        int maxRetry,
                        boolean insecureSkipVerify) {
        }

        // 4. Lớp con cho JWT
        public record JwtProperties(
                        @NotNull Resource accessTokenPublicKey,
                        @NotNull Resource accessTokenPrivateKey,
                        @NotNull Duration accessTokenDuration) {
        }

        // 5. Lớp con cho Email
        public record EmailProperties(
                        @NotBlank @jakarta.validation.constraints.Email String senderEmail,
                        @NotBlank String senderPassword,
                        @NotBlank String linkVerifyEmail,
                        @NotBlank String linkJoinOrganization,
                        @NotBlank String linkResetPassword,
                        @NotBlank String templateVerifyEmail,
                        @NotBlank String templateResetPassword,
                        @NotBlank String templateOrganizationInv) {
        }

        // 6. Lớp con cho Reset Password
        public record ResetPasswordProperties(
                        @NotNull Duration tokenTtl,
                        @NotNull Resource privateKey,
                        @NotNull Resource publicKey) {
        }
}
