CREATE DATABASE IF NOT EXISTS real_estate_db;
USE real_estate_db;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30),
    role_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    property_type VARCHAR(50) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    bedrooms INT,
    bathrooms INT,
    area_sqft INT,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    approval_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    agent_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_properties_agent FOREIGN KEY (agent_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS property_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorites_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_favorites_property FOREIGN KEY (property_id) REFERENCES properties(id),
    UNIQUE (customer_id, property_id)
);

CREATE TABLE IF NOT EXISTS inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    response_message TEXT,
    responded_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inquiries_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_inquiries_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE TABLE IF NOT EXISTS property_visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    scheduled_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visits_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_visits_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_visits_agent FOREIGN KEY (agent_id) REFERENCES users(id)
);

INSERT IGNORE INTO roles (name) VALUES ('ADMIN'), ('AGENT'), ('CUSTOMER');
