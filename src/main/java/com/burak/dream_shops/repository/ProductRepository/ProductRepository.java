package com.burak.dream_shops.repository.ProductRepository;

import com.burak.dream_shops.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product,Long> {
    List<Product> findByCategoryName(String category);
    List<Product> findByBrandAndName(String brand,String name);
    List<Product> findByBrand(String brand);
    List<Product> findByCategoryAndBrand(String category,String brand);

    List<Product> findByName(String name);

    Long countByBrandAndName(String brand, String name);
}
