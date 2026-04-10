CREATE TABLE USER_ACCOUNT (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    /* 
    *  The role will dictate exactly what data they are allowed to see and modify
    *  ADMIN - high-level access, allows for modification of the database.
    *  TENANT - low-level, extremely restricted read-only access to the database.
    */
    role VARCHAR(10) NOT NULL,
    CONSTRAINT chk_role CHECK(role IN ('ADMIN', 'TENANT'))
);

CREATE TABLE TENANT (
    tenant_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    contact_number VARCHAR(20),
    email VARCHAR(50),
    total_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deleted_at TIMESTAMP DEFAULT NULL, -- allows us to soft delete a tenant to preserve records.
    FOREIGN KEY (user_id) REFERENCES USER_ACCOUNT(user_id) ON DELETE SET NULL
    -- A tenant's balance may be negative if they pre-pay, so no check constraint is added
);

CREATE TABLE ROOM (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type VARCHAR(10) NOT NULL,
    capacity INT NOT NULL,
    current_occupancy INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_capacity CHECK (current_occupancy <= capacity)
);

CREATE TABLE LEASE (
    lease_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    room_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT chk_dates CHECK (start_date < end_date),
    CONSTRAINT chk_lease_status CHECK (status IN('ACTIVE', 'EXPIRED', 'TERMINATED'))
);


CREATE TABLE PAYMENT (
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

CREATE TABLE MAINTENANCE_REQUEST (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    room_id INT, -- Allowed to be null because tenants may submit a request for shared areas.
    report_description TEXT NOT NULL,
    reported_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    FOREIGN KEY (room_id) REFERENCES ROOM(room_id) ON DELETE SET NULL,
    CONSTRAINT chk_maintenance_status CHECK (status IN ('NEW', 'IN-PROGRESS', 'RESOLVED'))
);

CREATE TABLE DOCUMENT (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id)
);

-- Triggers
-- auto update the balance after every payment
DELIMITER //
CREATE TRIGGER update_tenant_balance_after_payment
AFTER INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    UPDATE TENANT
    SET total_balance = total_balance - NEW.amount_paid
    WHERE tenant_id = NEW.tenant_id
END; //
DELIMITER ;

-- auto increment the capacity after every new active lease
DELIMITER //
CREATE TRIGGER increment_occupancy
AFTER INSERT ON LEASE
FOR EACH ROW
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        UPDATE ROOM
        SET current_occupancy = current_occupancy + 1
        WHERE room_id = NEW.room_id;

-- dynamically check for expired leases
SET GLOBAL event_scheduler = ON;

CREATE EVENT update_leases_daily
ON SCHEDULE EVERY 1 DAY 
STARTS(CURRENT_DATE + INTERVAL 1 DAY)
DO
    UPDATE LEASE
    SET status = 'EXPIRED'
    WHERE end_date < CURRENT_DATE() and STATUS = 'ACTIVE';

-- insert test users
INSERT INTO USER_ACCOUNT (username, password_hash, role)
VALUES ('admin', '123', 'ADMIN');
INSERT INTO USER_ACCOUNT (username, password_hash, role)
VALUES ('tenant', '123', 'TENANT');
