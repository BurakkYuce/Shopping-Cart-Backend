package com.datapulse.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "category_id")
    private String categoryId;

    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(columnDefinition = "TEXT")
    private String description;
}
