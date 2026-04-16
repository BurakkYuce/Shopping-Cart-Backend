package com.datapulse.dto.response;

import com.datapulse.model.Question;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionResponse {
    private String id;
    private String productId;
    private String userId;
    private String askerDisplayName;
    private String question;
    private String answer;
    private String answeredByUserId;
    private String answeredByDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    public static QuestionResponse from(Question q) {
        QuestionResponse r = new QuestionResponse();
        r.id = q.getId();
        r.productId = q.getProductId();
        r.userId = q.getUserId();
        r.askerDisplayName = maskEmail(q.getUser() != null ? q.getUser().getEmail() : null);
        r.question = q.getQuestion();
        r.answer = q.getAnswer();
        r.answeredByUserId = q.getAnsweredByUserId();
        r.answeredByDisplayName = maskEmail(q.getAnsweredBy() != null ? q.getAnsweredBy().getEmail() : null);
        r.createdAt = q.getCreatedAt();
        r.answeredAt = q.getAnsweredAt();
        return r;
    }

    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(at, 0));
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String head = local.substring(0, Math.min(2, local.length()));
        return head + "***" + domain;
    }
}
