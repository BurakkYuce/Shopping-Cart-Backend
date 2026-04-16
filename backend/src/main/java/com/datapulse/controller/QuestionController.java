package com.datapulse.controller;

import com.datapulse.dto.request.AnswerQuestionRequest;
import com.datapulse.dto.request.CreateQuestionRequest;
import com.datapulse.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<?> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(questionService.getByProductId(productId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> ask(
            @Valid @RequestBody CreateQuestionRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.ask(request, auth));
    }

    @PatchMapping("/{id}/answer")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> answer(
            @PathVariable String id,
            @Valid @RequestBody AnswerQuestionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(questionService.answer(id, request, auth));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        questionService.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}
