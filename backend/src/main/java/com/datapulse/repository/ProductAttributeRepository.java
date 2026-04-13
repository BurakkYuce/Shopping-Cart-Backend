package com.datapulse.repository;

import com.datapulse.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, String> {
    List<ProductAttribute> findByProductId(String productId);
    Optional<ProductAttribute> findByProductIdAndAttrKey(String productId, String attrKey);
    void deleteByProductIdAndAttrKey(String productId, String attrKey);
}
