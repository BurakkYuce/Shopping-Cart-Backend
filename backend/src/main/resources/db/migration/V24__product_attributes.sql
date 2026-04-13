CREATE TABLE IF NOT EXISTS product_attributes (
    id          varchar(8) PRIMARY KEY,
    product_id  varchar(255) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attr_key    varchar(100) NOT NULL,
    attr_value  varchar(500),
    UNIQUE (product_id, attr_key)
);

CREATE INDEX IF NOT EXISTS idx_product_attributes_product_id ON product_attributes(product_id);
CREATE INDEX IF NOT EXISTS idx_product_attributes_key ON product_attributes(attr_key);
