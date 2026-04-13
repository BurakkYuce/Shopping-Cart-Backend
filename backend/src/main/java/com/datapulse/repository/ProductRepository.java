package com.datapulse.repository;

import com.datapulse.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStoreId(String storeId);
    List<Product> findByStoreIdIn(List<String> storeIds);

    @Query("SELECT p FROM Product p WHERE p.storeId = :storeId AND p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockByStoreId(@Param("storeId") String storeId);
    Page<Product> findAll(Pageable pageable);
    Page<Product> findByStoreIdIn(List<String> storeIds, Pageable pageable);

    @Query(value = "SELECT * FROM products p WHERE " +
           "(CAST(:query AS text) IS NULL OR p.name ILIKE ('%' || CAST(:query AS text) || '%') OR p.description ILIKE ('%' || CAST(:query AS text) || '%')) AND " +
           "(CAST(:categoryId AS text) IS NULL OR p.category_id = CAST(:categoryId AS text)) AND " +
           "(CAST(:brand AS text) IS NULL OR p.brand ILIKE CAST(:brand AS text)) AND " +
           "(CAST(:minPrice AS double precision) IS NULL OR p.unit_price >= CAST(:minPrice AS double precision)) AND " +
           "(CAST(:maxPrice AS double precision) IS NULL OR p.unit_price <= CAST(:maxPrice AS double precision))",
           countQuery = "SELECT count(*) FROM products p WHERE " +
           "(CAST(:query AS text) IS NULL OR p.name ILIKE ('%' || CAST(:query AS text) || '%') OR p.description ILIKE ('%' || CAST(:query AS text) || '%')) AND " +
           "(CAST(:categoryId AS text) IS NULL OR p.category_id = CAST(:categoryId AS text)) AND " +
           "(CAST(:brand AS text) IS NULL OR p.brand ILIKE CAST(:brand AS text)) AND " +
           "(CAST(:minPrice AS double precision) IS NULL OR p.unit_price >= CAST(:minPrice AS double precision)) AND " +
           "(CAST(:maxPrice AS double precision) IS NULL OR p.unit_price <= CAST(:maxPrice AS double precision))",
           nativeQuery = true)
    Page<Product> search(
            @Param("query") String query,
            @Param("categoryId") String categoryId,
            @Param("brand") String brand,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);
}
