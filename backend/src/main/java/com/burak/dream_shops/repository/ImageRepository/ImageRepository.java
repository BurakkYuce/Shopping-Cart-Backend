package com.burak.dream_shops.repository.ImageRepository;

import com.burak.dream_shops.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image,Long> {
    List<Image> findByProductId(Long id);

}
