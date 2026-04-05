package com.datapulse.dto.response;

import com.datapulse.model.Category;
import lombok.Data;

@Data
public class CategoryResponse {
    private String id;
    private String name;
    private String parentId;

    public static CategoryResponse from(Category category) {
        CategoryResponse r = new CategoryResponse();
        r.id = category.getId();
        r.name = category.getName();
        r.parentId = category.getParentId();
        return r;
    }
}
