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

CREATE TABLE ANNOUNCEMENT (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    date_posted TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO ANNOUNCEMENT (title, message)
VALUES
('Water Interruption', 'Water will be unavailable tomorrow from 2:00 PM to 4:00 PM due to maintenance.'),
('Pest Control Schedule', 'Monthly pest control will be conducted this Friday morning. Please secure your pets.');

CREATE TABLE ACTIVITY_LOG (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id) ON DELETE CASCADE
);

DELIMITER //
CREATE TRIGGER update_tenant_balance_after_payment
AFTER INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    UPDATE TENANT
    SET total_balance = total_balance - NEW.amount_paid
    WHERE tenant_id = NEW.tenant_id;
END //
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
    END IF;
END //

SET GLOBAL event_scheduler = ON;

CREATE EVENT update_leases_daily
ON SCHEDULE EVERY 1 DAY 
STARTS(CURRENT_DATE + INTERVAL 1 DAY)
DO
    UPDATE LEASE
    SET status = 'EXPIRED'
    WHERE end_date < CURRENT_DATE() and STATUS = 'ACTIVE';

DELIMITER //

CREATE TRIGGER after_payment_insert
AFTER INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    -- This automatically logs: "Submitted a payment of ₱5000.00"
    INSERT INTO ACTIVITY_LOG (tenant_id, description)
    VALUES (NEW.tenant_id, CONCAT('Submitted a payment of ₱', FORMAT(NEW.amount_paid, 2)));
END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER after_maintenance_insert
AFTER INSERT ON MAINTENANCE_REQUEST
FOR EACH ROW
BEGIN
    -- This automatically logs: "Submitted maintenance request: Leaky faucet in kitchen"
    INSERT INTO ACTIVITY_LOG (tenant_id, description)
    VALUES (NEW.tenant_id, CONCAT('Submitted maintenance request: ', NEW.report_description));
END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER after_payment_update
AFTER UPDATE ON PAYMENT
FOR EACH ROW
BEGIN
    -- Only log it if the status actually changed!
    IF OLD.status != NEW.status THEN
        INSERT INTO ACTIVITY_LOG (tenant_id, description)
        VALUES (NEW.tenant_id, CONCAT('Payment #', NEW.payment_id, ' was marked as ', NEW.status));
    END IF;
END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER after_maintenance_update
AFTER UPDATE ON MAINTENANCE_REQUEST
FOR EACH ROW
BEGIN
    -- Only log it if the status actually changed!
    IF OLD.status != NEW.status THEN
        INSERT INTO ACTIVITY_LOG (tenant_id, description)
        VALUES (NEW.tenant_id, CONCAT('Maintenance request #', NEW.request_id, ' was updated to ', NEW.status));
    END IF;
END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER after_tenant_lease_update
AFTER UPDATE ON LEASE
FOR EACH ROW
BEGIN
    -- Assuming you have a column named lease_status
    IF OLD.status != NEW.status THEN
        INSERT INTO ACTIVITY_LOG (tenant_id, description)
        VALUES (NEW.tenant_id, CONCAT('Lease status updated to: ', NEW.status));
    END IF;
END //

DELIMITER ;

INSERT INTO USER_ACCOUNT (username, password_hash, role)
VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN');
INSERT INTO USER_ACCOUNT (username, password_hash, role)
VALUES ('tenant', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f1979c67f', 'TENANT');

ALTER TABLE ROOM ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE LEASE ADD COLUMN monthly_rent DECIMAL(10,2) NOT NULL;

ALTER TABLE ROOM ADD COLUMN floor TINYINT NOT NULL DEFAULT 1 AFTER room_number;