package com.datapulse.controller;

import com.datapulse.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<?> listConversations(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return ResponseEntity.ok(conversationService.listConversations(auth, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConversation(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(conversationService.getConversation(id, auth));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<?> updateTitle(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return ResponseEntity.ok(conversationService.updateTitle(id, body.get("title"), auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String id, Authentication auth) {
        conversationService.deleteConversation(id, auth);
        return ResponseEntity.noContent().build();
    }
}
