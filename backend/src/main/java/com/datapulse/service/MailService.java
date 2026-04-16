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

    @Async
    public void sendWeeklyNewsletterEmail(String toEmail, int newArrivalsCount, int activePromos) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(toEmail);
        msg.setSubject("DataPulse — The Curated Dispatch, this week");
        msg.setText(
                "Hi,\n\n" +
                "Here is your weekly curated dispatch:\n\n" +
                "• " + newArrivalsCount + " fresh arrivals from curators you follow\n" +
                "• " + activePromos + " active promotions waiting for you\n\n" +
                "Browse the latest: " + appBaseUrl + "/products\n\n" +
                "You can turn off the newsletter in Account → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("Weekly newsletter sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send newsletter to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendNewOrderToSellerEmail(String sellerEmail, String orderId, String buyerName, String total) {
        String link = appBaseUrl + "/seller/orders";
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(sellerEmail);
        msg.setSubject("DataPulse — New order #" + orderId);
        msg.setText(
                "Hi,\n\n" +
                "You have a new order from " + buyerName + ".\n" +
                "Order ID: " + orderId + "\n" +
                "Total: " + total + "\n\n" +
                "Manage it here: " + link + "\n\n" +
                "You can turn off new-order emails in Seller Settings → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("New-order email sent to seller {} (order {})", sellerEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send new-order email to {}: {}", sellerEmail, e.getMessage());
        }
    }

    @Async
    public void sendLowStockEmail(String sellerEmail, String productName, int remaining, int threshold) {
        String link = appBaseUrl + "/seller/products";
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(sellerEmail);
        msg.setSubject("DataPulse — Low stock: " + productName);
        msg.setText(
                "Hi,\n\n" +
                "Heads up — one of your products is running low:\n\n" +
                "Product: " + productName + "\n" +
                "Remaining: " + remaining + " (threshold: " + threshold + ")\n\n" +
                "Restock or adjust here: " + link + "\n\n" +
                "You can turn off low-stock alerts in Seller Settings → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("Low-stock email sent to {} (product {}, remaining {})", sellerEmail, productName, remaining);
        } catch (Exception e) {
            log.error("Failed to send low-stock email to {}: {}", sellerEmail, e.getMessage());
        }
    }

    @Async
    public void sendNewReviewEmail(String sellerEmail, String productName, int stars, String preview) {
        String link = appBaseUrl + "/seller/reviews";
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(sellerEmail);
        msg.setSubject("DataPulse — New review on " + productName + " (" + stars + "★)");
        msg.setText(
                "Hi,\n\n" +
                "A customer just reviewed " + productName + ".\n" +
                "Rating: " + stars + "★\n" +
                (preview != null && !preview.isBlank() ? "Excerpt: \"" + preview + "\"\n\n" : "\n") +
                "Reply or manage here: " + link + "\n\n" +
                "You can turn off review alerts in Seller Settings → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("New-review email sent to {} (product {}, stars {})", sellerEmail, productName, stars);
        } catch (Exception e) {
            log.error("Failed to send new-review email to {}: {}", sellerEmail, e.getMessage());
        }
    }

    @Async
    public void sendWeeklyStoreDigestEmail(String sellerEmail, String storeName,
                                           long orders, String revenue, long newReviews, long lowStockCount) {
        String link = appBaseUrl + "/seller/dashboard";
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + from + ">");
        msg.setTo(sellerEmail);
        msg.setSubject("DataPulse — Weekly digest for " + storeName);
        msg.setText(
                "Hi,\n\n" +
                "Here is last week at " + storeName + ":\n\n" +
                "• Orders: " + orders + "\n" +
                "• Revenue: " + revenue + "\n" +
                "• New reviews: " + newReviews + "\n" +
                "• Low-stock products: " + lowStockCount + "\n\n" +
                "Full dashboard: " + link + "\n\n" +
                "You can turn off the weekly digest in Seller Settings → Notifications.\n\n" +
                "— DataPulse");
        try {
            mailSender.send(msg);
            log.info("Weekly digest sent to {} (store {})", sellerEmail, storeName);
        } catch (Exception e) {
            log.error("Failed to send weekly digest to {}: {}", sellerEmail, e.getMessage());
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
