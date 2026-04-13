package com.datapulse.repository;

import com.datapulse.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, String> {
    Optional<Brand> findBySlug(String slug);
    List<Brand> findByDisplayNameContainingIgnoreCase(String name);
}
