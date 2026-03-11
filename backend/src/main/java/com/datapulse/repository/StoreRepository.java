package com.datapulse.repository;

import com.datapulse.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, String> {
    List<Store> findByOwnerId(String ownerId);
}
