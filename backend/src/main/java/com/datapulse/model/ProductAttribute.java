package com.datapulse.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "product_attributes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "attr_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {

    @Id
    private String id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "attr_key", nullable = false, length = 100)
    private String attrKey;

    @Column(name = "attr_value", length = 500)
    private String attrValue;
}
