package com.genifast.dms.common.service;

import lombok.RequiredArgsConstructor;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.genifast.dms.common.dto.EmailData;
import com.genifast.dms.common.dto.ResetPasswordInfo;
import com.genifast.dms.common.dto.VerifyEmailInfo;
import com.genifast.dms.config.ApplicationProperties;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    private final ApplicationProperties applicationProperties;

    /**
     * Gửi mail mời tham gia tổ chức.
     * Template: organization_invitation.html
     */
    public void sendOrganizationInvitation(String to, EmailData data) {
        sendTemplateEmail(to, applicationProperties.email().templateOrganizationInv(), context -> {
            context.setVariable("organizationName", data.organizationName());
            context.setVariable("joinLink", data.joinLink());
        }, "GENADATA Invitation");
    }

    public void sendResetPasswordLink(String to, ResetPasswordInfo data) {
        String resetLink = String.format("%s?token=%s", data.resetLink(), data.token());

        sendTemplateEmail(to, applicationProperties.email().templateResetPassword(),
                context -> context.setVariable("resetLink", resetLink),
                "GENADATA Reset Password");
    }

    public void sendVerifyEmailCreateAccount(String to, VerifyEmailInfo data) {
        String verifyLink = String.format("%s?email=%s", data.verifyLink(), to);

        sendTemplateEmail(to, applicationProperties.email().templateVerifyEmail(),
                context -> context.setVariable("verifyLink", verifyLink),
                "GENADATA Verify Email");
    }

    @Async
    private void sendTemplateEmail(String to,
            String templateName,
            java.util.function.Consumer<Context> contextSetter,
            String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);

            Context ctx = new Context();
            contextSetter.accept(ctx);
            String html = templateEngine.process(templateName, ctx);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email: " + subject, ex);
        }
    }

    /**
     * Gửi một email đơn giản không dùng template.
     * 
     * @param to      Địa chỉ email người nhận.
     * @param subject Tiêu đề email.
     * @param text    Nội dung email.
     */
    @Async
    public void sendSimpleNotification(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            // Trong thực tế, nên log lỗi này chi tiết hơn
            throw new IllegalStateException("Failed to send simple email to " + to, ex);
        }
    }

    /**
     * Gửi email thông báo về tài liệu, sử dụng template.
     * 
     * @param to            Email người nhận
     * @param recipientName Tên người nhận (để chào hỏi)
     * @param subject       Tiêu đề của email
     * @param message       Nội dung chính của thông báo (có thể chứa HTML)
     */
    @Async
    public void sendDocumentNotification(String to, String recipientName, String subject, String message) {
        sendTemplateEmail(to, "document_notification", context -> {
            context.setVariable("recipientName", recipientName);
            context.setVariable("subject", subject);
            context.setVariable("message", message);
        }, subject);
    }
}
