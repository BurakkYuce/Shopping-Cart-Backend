-- V1: Initial schema for DataPulse E-Commerce Platform

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_type VARCHAR(255) NOT NULL CHECK (role_type IN ('ADMIN', 'CORPORATE', 'INDIVIDUAL')),
    gender VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id VARCHAR(255),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS stores (
    id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    CONSTRAINT fk_stores_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(255) PRIMARY KEY,
    store_id VARCHAR(255) NOT NULL,
    category_id VARCHAR(255),
    sku VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    unit_price DOUBLE PRECISION,
    description TEXT,
    CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    store_id VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    grand_total DOUBLE PRECISION,
    created_at TIMESTAMP(6),
    payment_method VARCHAR(255),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER,
    price DOUBLE PRECISION,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    star_rating INTEGER,
    helpful_votes INTEGER,
    total_votes INTEGER,
    review_headline VARCHAR(255),
    review_text TEXT,
    sentiment VARCHAR(255),
    verified_purchase VARCHAR(255),
    review_date DATE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS shipments (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    warehouse VARCHAR(255),
    mode VARCHAR(255),
    status VARCHAR(255),
    customer_care_calls INTEGER,
    customer_rating INTEGER,
    weight_gms INTEGER,
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS customer_profiles (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    age INTEGER,
    city VARCHAR(255),
    membership_type VARCHAR(255),
    total_spend DOUBLE PRECISION,
    items_purchased INTEGER,
    average_rating DOUBLE PRECISION,
    satisfaction_level VARCHAR(255),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);
