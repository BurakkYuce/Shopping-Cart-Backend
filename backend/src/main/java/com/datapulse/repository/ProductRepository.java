package com.datapulse.repository;

import com.datapulse.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStoreId(String storeId);
    List<Product> findByStoreIdIn(List<String> storeIds);
    Page<Product> findAll(Pageable pageable);
    Page<Product> findByStoreIdIn(List<String> storeIds, Pageable pageable);
}
