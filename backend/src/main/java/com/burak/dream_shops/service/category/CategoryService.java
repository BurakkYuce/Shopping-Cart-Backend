package com.burak.dream_shops.service.category;

import com.burak.dream_shops.exceptions.AlreadyExistsException;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Category;
import com.burak.dream_shops.repository.CategoryRepository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Kategori (Category) işlemlerini yöneten servis sınıfı.
 * Kategori ekleme, güncelleme, silme ve listeleme işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {
    private final CategoryRepository categoryRepository;

    /** ID'ye göre kategori getirir. Bulunamazsa ResourcesNotFoundException fırlatır. */
    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Category Not Found!"));
    }

    /** İsme göre kategori getirir. Bulunamazsa null döner. */
    @Override
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    /** Tüm kategorileri listeler. */
    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Yeni kategori ekler.
     * Aynı isimde kategori varsa AlreadyExistsException fırlatır.
     */
    @Override
    public Category addCategory(Category category) {
        return Optional.of(category)
                .filter(c -> !categoryRepository.existsByName(c.getName()))
                .map(categoryRepository::save)
                .orElseThrow(() -> new AlreadyExistsException(category.getName() + " already exists"));
    }

    /**
     * Var olan kategoriyi günceller.
     * ID ile mevcut kategoriyi bulur, adını değiştirir ve kaydeder.
     */
    @Override
    public Category updateCategory(Category category, Long id) {
        return Optional.ofNullable(getCategoryById(id))
                .map(oldCategory -> {
                    oldCategory.setName(category.getName());
                    return categoryRepository.save(oldCategory);
                })
                .orElseThrow(() -> new ResourcesNotFoundException("Category not found!"));
    }

    /** ID'ye göre kategori siler. Bulunamazsa ResourcesNotFoundException fırlatır. */
    @Override
    public void deleteCategoryById(Long id) {
        categoryRepository.findById(id)
                .ifPresentOrElse(categoryRepository::delete, () -> {
                    throw new ResourcesNotFoundException("Category Not Found!");
                });
    }
}
