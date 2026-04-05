package com.datapulse.service;

import com.datapulse.dto.response.CategoryResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.Category;
import com.datapulse.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse getById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIdIsNull().stream().map(CategoryResponse::from).toList();
    }

    public List<CategoryResponse> getChildren(String parentId) {
        return categoryRepository.findByParentId(parentId).stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse create(Category category) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        category.setId(id);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse update(String id, Category updates) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));

        if (updates.getName() != null) {
            category.setName(updates.getName());
        }
        if (updates.getParentId() != null) {
            category.setParentId(updates.getParentId());
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
        categoryRepository.delete(category);
    }
}
