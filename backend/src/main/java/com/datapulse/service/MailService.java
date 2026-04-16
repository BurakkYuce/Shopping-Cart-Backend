package com.datapulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.app-base-url}")
    private String appBaseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String link = appBaseUrl + "/verify-email?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(toEmail);
        msg.setSubject("DataPulse — E-posta Adresinizi Doğrulayın");
        msg.setText(
                "Merhaba,\n\n" +
                "DataPulse hesabınızı doğrulamak için aşağıdaki bağlantıya tıklayın:\n\n" +
                link + "\n\n" +
                "Bu bağlantı 24 saat boyunca geçerlidir.\n\n" +
                "Bu e-postayı siz talep etmediyseniz dikkate almayın.\n\n" +
                "— DataPulse ekibi");
        try {
            mailSender.send(msg);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    @Async
    public void sendOrderStatusEmail(String toEmail, String orderId, String newStatus) {
        String link = appBaseUrl + "/account/orders/" + orderId;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(toEmail);
        msg.setSubject("DataPulse — Order #" + orderId + " is now " + newStatus);
        msg.setText(
                "Hi,\n\n" +
                "Your order " + orderId + " status was updated to: " + newStatus + ".\n\n" +
                "View details: " + link + "\n\n" +
                "You can turn off order update emails in Account → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("Order-status email sent to {} (order {})", toEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send order-status email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendNewArrivalEmail(String toEmail, String productName, String productId) {
        String link = appBaseUrl + "/products/" + productId;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(toEmail);
        msg.setSubject("DataPulse — New arrival: " + productName);
        msg.setText(
                "Hi,\n\n" +
                "A new product just landed: " + productName + "\n\n" +
                link + "\n\n" +
                "You can turn off new-arrival emails in Account → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("New-arrival email sent to {} (product {})", toEmail, productId);
        } catch (Exception e) {
            log.error("Failed to send new-arrival email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPromotionEmail(String toEmail, String code, String description) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(toEmail);
        msg.setSubject("DataPulse — New promotion: " + code);
        msg.setText(
                "Hi,\n\n" +
                "Use code " + code + " at checkout.\n" +
                (description != null && !description.isBlank() ? description + "\n\n" : "\n") +
                "Browse: " + appBaseUrl + "/products\n\n" +
                "You can turn off promotion emails in Account → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("Promotion email sent to {} (code {})", toEmail, code);
        } catch (Exception e) {
            log.error("Failed to send promotion email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = appBaseUrl + "/auth/reset-password?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(toEmail);
        msg.setSubject("DataPulse — Şifre Sıfırlama");
        msg.setText(
                "Merhaba,\n\n" +
                "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın:\n\n" +
                link + "\n\n" +
                "Bu bağlantı 1 saat boyunca geçerlidir.\n\n" +
                "Bu e-postayı siz talep etmediyseniz dikkate almayın.\n\n" +
                "— DataPulse ekibi");
        try {
            mailSender.send(msg);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
