CREATE TABLE USER_ACCOUNT (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'TENANT'))
);

CREATE TABLE TENANT (
    tenant_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT,
    first_name     VARCHAR(50) NOT NULL,
    last_name      VARCHAR(50) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    email          VARCHAR(50),
    deleted_at     TIMESTAMP DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES USER_ACCOUNT(user_id) ON DELETE SET NULL
);

CREATE TABLE ROOM (
    room_id     INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type   VARCHAR(10) NOT NULL,
    capacity    INT         NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_capacity CHECK (capacity > 0)
    -- Occupancy is derived: SELECT COUNT(*) FROM LEASE
    --   WHERE room_id = ? AND status = 'ACTIVE'
);

CREATE TABLE LEASE (
    lease_id     INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id    INT            NOT NULL,
    room_id      INT            NOT NULL,
    start_date   DATE           NOT NULL,
    end_date     DATE           NOT NULL,
    monthly_rent DECIMAL(10,2)  NOT NULL,
    balance      DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    status       VARCHAR(10)    NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    FOREIGN KEY (room_id)   REFERENCES ROOM(room_id),
    CONSTRAINT chk_lease_dates   CHECK (end_date > start_date),
    CONSTRAINT chk_monthly_rent  CHECK (monthly_rent > 0),
    CONSTRAINT chk_lease_status  CHECK (status IN ('ACTIVE', 'EXPIRED', 'TERMINATED'))
);

CREATE TABLE CHARGE (
    charge_id    INT AUTO_INCREMENT PRIMARY KEY,
    lease_id     INT           NOT NULL,
    charge_type  VARCHAR(20)   NOT NULL,          -- 'RENT', 'LATE_FEE', 'UTILITY', etc.
    amount       DECIMAL(10,2) NOT NULL,
    due_date     DATE          NOT NULL,
    description  VARCHAR(255),
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lease_id) REFERENCES LEASE(lease_id),
    CONSTRAINT chk_charge_type CHECK (charge_type IN ('RENT', 'LATE_FEE', 'DEPOSIT', 'UTILITY')),
    CONSTRAINT chk_charge_amount CHECK (amount > 0)
);

CREATE TABLE PAYMENT (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    lease_id       INT           NOT NULL,   -- replaces tenant_id; tenant is reachable via lease
    amount_paid    DECIMAL(10,2) NOT NULL,
    payment_date   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20),
    status         VARCHAR(20)   NOT NULL,
    FOREIGN KEY (lease_id) REFERENCES LEASE(lease_id),
    CONSTRAINT chk_amount         CHECK (amount_paid > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'VERIFIED', 'FAILED'))
);

CREATE TABLE MAINTENANCE_REQUEST (
    request_id         INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id          INT  NOT NULL,
    room_id            INT,
    report_description TEXT NOT NULL,
    reported_date      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status             VARCHAR(20) NOT NULL DEFAULT 'NEW',
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id),
    FOREIGN KEY (room_id)   REFERENCES ROOM(room_id) ON DELETE SET NULL,
    CONSTRAINT chk_maintenance_status CHECK (status IN ('NEW', 'IN-PROGRESS', 'RESOLVED'))
);

CREATE TABLE DOCUMENT (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   INT          NOT NULL,
    title       VARCHAR(255) NOT NULL,
    file_path   VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id)
);

CREATE TABLE ANNOUNCEMENT (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100) NOT NULL,
    message         TEXT         NOT NULL,
    date_posted     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ACTIVITY_LOG (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   INT          NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES TENANT(tenant_id) ON DELETE CASCADE
);

-- TRIGGERS --
-- -------------------------------------------------------------------------
-- Automatically update LEASE.balance when a payment is inserted
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_after_payment_insert
AFTER INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    IF NEW.status = 'VERIFIED' THEN
        UPDATE LEASE
        SET balance = balance - NEW.amount_paid
        WHERE lease_id = NEW.lease_id;
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Re-adjust LEASE.balance if a payment status changes (e.g. PENDING → VERIFIED,
-- or VERIFIED → FAILED)
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_after_payment_update
AFTER UPDATE ON PAYMENT
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        -- Payment just got verified: subtract from balance
        IF NEW.status = 'VERIFIED' AND OLD.status != 'VERIFIED' THEN
            UPDATE LEASE
            SET balance = balance - NEW.amount_paid
            WHERE lease_id = NEW.lease_id;

        -- Payment was reversed/failed after being verified: add it back
        ELSEIF OLD.status = 'VERIFIED' AND NEW.status != 'VERIFIED' THEN
            UPDATE LEASE
            SET balance = balance + NEW.amount_paid
            WHERE lease_id = NEW.lease_id;
        END IF;
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Log payment submission — tenant_id is fetched through LEASE
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_log_payment_insert
AFTER INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    INSERT INTO ACTIVITY_LOG (tenant_id, description)
    SELECT tenant_id, CONCAT('Submitted a payment of ₱', FORMAT(NEW.amount_paid, 2))
    FROM LEASE
    WHERE lease_id = NEW.lease_id;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Log payment status changes — tenant_id fetched through LEASE
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_log_payment_update
AFTER UPDATE ON PAYMENT
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO ACTIVITY_LOG (tenant_id, description)
        SELECT tenant_id, CONCAT('Payment #', NEW.payment_id, ' status changed to ', NEW.status)
        FROM LEASE
        WHERE lease_id = NEW.lease_id;
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Log maintenance request submission
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_log_maintenance_insert
AFTER INSERT ON MAINTENANCE_REQUEST
FOR EACH ROW
BEGIN
    INSERT INTO ACTIVITY_LOG (tenant_id, description)
    VALUES (NEW.tenant_id, CONCAT('Submitted a maintenance request: ', LEFT(NEW.report_description, 100)));
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Log maintenance request status changes
-- Only fires if status actually changed (was already there, kept it)
-- Also trims the description in case it's very long
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_log_maintenance_update
AFTER UPDATE ON MAINTENANCE_REQUEST
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO ACTIVITY_LOG (tenant_id, description)
        VALUES (NEW.tenant_id, CONCAT('Maintenance request #', NEW.request_id, ' updated to ', NEW.status));
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Log lease status changes
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_log_lease_update
AFTER UPDATE ON LEASE
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO ACTIVITY_LOG (tenant_id, description)
        VALUES (NEW.tenant_id, CONCAT('Lease #', NEW.lease_id, ' status updated to ', NEW.status));
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Prevent overlapping active leases for the same room
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_prevent_lease_overlap
BEFORE INSERT ON LEASE
FOR EACH ROW
BEGIN
    DECLARE active_tenants INT;
    DECLARE room_capacity INT;

    SELECT COUNT(*) INTO active_tenants
    FROM LEASE
    WHERE room_id = NEW.room_id
      AND status = 'ACTIVE'
      AND start_date < NEW.end_date
      AND end_date > NEW.start_date;

    SELECT capacity INTO room_capacity
    FROM ROOM
    WHERE room_id = NEW.room_id;

    IF active_tenants >= room_capacity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Room has reached maximum occupancy for this period';
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- When a CHARGE is added to a lease, increase the balance accordingly
-- Without this, adding rent/fees won't reflect on the balance at all
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_after_charge_insert
AFTER INSERT ON CHARGE
FOR EACH ROW
BEGIN
    UPDATE LEASE
    SET balance = balance + NEW.amount
    WHERE lease_id = NEW.lease_id;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- If a charge amount is edited (e.g. admin corrects a mistake),
-- adjust the balance by the difference
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_after_charge_update
AFTER UPDATE ON CHARGE
FOR EACH ROW
BEGIN
    IF OLD.amount != NEW.amount THEN
        UPDATE LEASE
        SET balance = balance + (NEW.amount - OLD.amount)
        WHERE lease_id = NEW.lease_id;
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- If a charge is deleted, remove it from the balance
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_after_charge_delete
AFTER DELETE ON CHARGE
FOR EACH ROW
BEGIN
    UPDATE LEASE
    SET balance = balance - OLD.amount
    WHERE lease_id = OLD.lease_id;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Auto-expire leases whose end_date has passed when they are touched
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_auto_expire_lease
BEFORE UPDATE ON LEASE
FOR EACH ROW
BEGIN
    IF NEW.status = 'ACTIVE' AND CURDATE() > NEW.end_date THEN
        SET NEW.status = 'EXPIRED';
    END IF;
END //
DELIMITER ;

CREATE EVENT evt_expire_leases
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
    UPDATE LEASE
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE'
      AND end_date < CURDATE();

-- -------------------------------------------------------------------------
-- Prevent a payment from being inserted against a non-ACTIVE lease with no 
-- balance 
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_prevent_unnecessary_payment
BEFORE INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    DECLARE lease_status VARCHAR(10);
    DECLARE lease_balance DECIMAL(10,2);

    SELECT status, balance INTO lease_status, lease_balance
    FROM LEASE
    WHERE lease_id = NEW.lease_id;

    -- Block payments on inactive leases only if balance is already cleared
    IF lease_status != 'ACTIVE' AND lease_balance <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot add a payment to a closed lease with no outstanding balance';
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Prevent a charge from being added to a non-ACTIVE lease with no balance
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_prevent_charge_on_inactive_lease
BEFORE INSERT ON CHARGE
FOR EACH ROW
BEGIN
    DECLARE lease_status VARCHAR(10);
    DECLARE lease_balance DECIMAL(10,2);

    SELECT status, balance INTO lease_status, lease_balance
    FROM LEASE
    WHERE lease_id = NEW.lease_id;

    -- Allow charges on inactive leases only if there's still an outstanding balance
    IF lease_status != 'ACTIVE' AND lease_balance <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot add a charge to a closed lease with no outstanding balance';
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Prevent soft-deleted tenants from being assigned a new lease
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_prevent_lease_for_deleted_tenant
BEFORE INSERT ON LEASE
FOR EACH ROW
BEGIN
    DECLARE is_deleted TIMESTAMP;

    SELECT deleted_at INTO is_deleted
    FROM TENANT
    WHERE tenant_id = NEW.tenant_id;

    IF is_deleted IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot create a lease for a deleted tenant';
    END IF;
END //
DELIMITER ;

-- -------------------------------------------------------------------------
-- Prevent soft-deleted tenants from submitting maintenance requests
-- -------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_prevent_maintenance_for_deleted_tenant
BEFORE INSERT ON MAINTENANCE_REQUEST
FOR EACH ROW
BEGIN
    DECLARE is_deleted TIMESTAMP;

    SELECT deleted_at INTO is_deleted
    FROM TENANT
    WHERE tenant_id = NEW.tenant_id;

    IF is_deleted IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot submit a maintenance request for a deleted tenant';
    END IF;
END //
DELIMITER ;
