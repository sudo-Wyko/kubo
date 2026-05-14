-- Idempotent Kubo schema (tables only — no triggers/events).
-- Use when kubo_db is missing or you need a clean, valid bootstrap.

CREATE DATABASE IF NOT EXISTS kubo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kubo_db;

CREATE TABLE IF NOT EXISTS USER_ACCOUNT (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL,
    CONSTRAINT chk_role CHECK(role IN ('ADMIN', 'TENANT'))
);

CREATE TABLE IF NOT EXISTS TENANT (
    tenant_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    contact_number VARCHAR(20),
    email VARCHAR(50),
    total_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deleted_at TIMESTAMP DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES USER_ACCOUNT(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ROOM (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type VARCHAR(10) NOT NULL,
    capacity INT NOT NULL,
    current_occupancy INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT chk_capacity CHECK (current_occupancy <= capacity)
);

CREATE TABLE IF NOT EXISTS LEASE (
    lease_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    room_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    charged_rent_periods INT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT chk_dates CHECK (start_date < end_date),
    CONSTRAINT chk_lease_status CHECK (status IN('ACTIVE', 'EXPIRED', 'TERMINATED'))
);

-- Existing DBs created from an older init (no charged_rent_periods): add column idempotently.
SET @kubo_lease_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'lease' AND COLUMN_NAME = 'charged_rent_periods'
);
SET @kubo_lease_sql := IF(
    @kubo_lease_col = 0,
    'ALTER TABLE LEASE ADD COLUMN charged_rent_periods INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE kubo_stmt FROM @kubo_lease_sql;
EXECUTE kubo_stmt;
DEALLOCATE PREPARE kubo_stmt;

CREATE TABLE IF NOT EXISTS PAYMENT (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    CONSTRAINT chk_amount CHECK (amount_paid > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'VERIFIED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS MAINTENANCE_REQUEST (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    room_id INT NULL,
    report_description TEXT NOT NULL,
    reported_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    FOREIGN KEY (room_id) REFERENCES ROOM(room_id) ON DELETE SET NULL,
    CONSTRAINT chk_maintenance_status CHECK (status IN ('NEW', 'IN-PROGRESS', 'RESOLVED'))
);

CREATE TABLE IF NOT EXISTS DOCUMENT (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id)
);
