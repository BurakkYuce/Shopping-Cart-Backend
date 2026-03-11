package com.datapulse.controller;

import com.datapulse.model.Category;
import com.datapulse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<?> getChildren(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getChildren(id));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Category body,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody Category body,
            Authentication auth) {
        return ResponseEntity.ok(categoryService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
