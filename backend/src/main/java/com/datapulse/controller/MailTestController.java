package com.datapulse.controller;

import com.datapulse.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Admin-only manual trigger for every notification email. Hit this once, open the inbox,
 *  eyeball the eight messages. Useful when smoke-testing SMTP changes or prompt edits. */
@RestController
@RequestMapping("/api/admin/mail-test")
@RequiredArgsConstructor
public class MailTestController {

    private final MailService mailService;

    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendAll(@RequestParam String email) {
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        String productId = UUID.randomUUID().toString().substring(0, 8);
        String token = UUID.randomUUID().toString().replace("-", "");

        Map<String, String> sent = new LinkedHashMap<>();

        try { mailService.sendVerificationEmail(email, token);                  sent.put("verification", "ok"); }
        catch (Exception e) { sent.put("verification", "error: " + e.getMessage()); }

        try { mailService.sendPasswordResetEmail(email, token);                 sent.put("passwordReset", "ok"); }
        catch (Exception e) { sent.put("passwordReset", "error: " + e.getMessage()); }

        try { mailService.sendOrderStatusEmail(email, orderId, "SHIPPED");      sent.put("orderStatus", "ok"); }
        catch (Exception e) { sent.put("orderStatus", "error: " + e.getMessage()); }

        try { mailService.sendNewArrivalEmail(email, "Linen Overshirt", productId);             sent.put("newArrival", "ok"); }
        catch (Exception e) { sent.put("newArrival", "error: " + e.getMessage()); }

        try { mailService.sendPromotionEmail(email, "SPRING20", "20% off seasonal curation");   sent.put("promotion", "ok"); }
        catch (Exception e) { sent.put("promotion", "error: " + e.getMessage()); }

        try { mailService.sendWeeklyNewsletterEmail(email, 12, 3);              sent.put("newsletter", "ok"); }
        catch (Exception e) { sent.put("newsletter", "error: " + e.getMessage()); }

        try { mailService.sendNewOrderToSellerEmail(email, orderId, "Test Buyer", "1299.00 TL"); sent.put("newOrderSeller", "ok"); }
        catch (Exception e) { sent.put("newOrderSeller", "error: " + e.getMessage()); }

        try { mailService.sendLowStockEmail(email, "Linen Overshirt", 3, 10);   sent.put("lowStock", "ok"); }
        catch (Exception e) { sent.put("lowStock", "error: " + e.getMessage()); }

        try { mailService.sendNewReviewEmail(email, "Linen Overshirt", 5,
                "Beautifully cut and the fabric breathes on warm days.");       sent.put("newReview", "ok"); }
        catch (Exception e) { sent.put("newReview", "error: " + e.getMessage()); }

        try { mailService.sendWeeklyStoreDigestEmail(email, "Atelier Nord",
                17L, "24,850.00 TL", 4L, 2L);                                   sent.put("weeklyStoreDigest", "ok"); }
        catch (Exception e) { sent.put("weeklyStoreDigest", "error: " + e.getMessage()); }

        return ResponseEntity.ok(Map.of(
                "to", email,
                "dispatched", sent,
                "note", "Async emails (order status, new arrival, promotion, newsletter, seller mails) " +
                        "are queued — check the inbox in a few seconds."
        ));
    }
}
