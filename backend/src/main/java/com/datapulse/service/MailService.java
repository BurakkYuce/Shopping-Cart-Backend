package com.datapulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
}
