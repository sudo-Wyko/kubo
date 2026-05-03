-- Demo accounts for Kubo (SHA-256 of passwords; matches LoginController + PasswordUtil).
-- Run against kubo_db, e.g.: mysql -u root -p kubo_db < scripts/seed-demo-accounts.sql
-- Or use: .\scripts\seed-demo-accounts.ps1
-- kubo_admin / KuboAdmin1!
-- kubo_tenant / KuboTenant1!

INSERT INTO USER_ACCOUNT (username, password_hash, role)
VALUES ('kubo_admin', '0bc11ac3399b777bc530b29150d38de686a41b03de356784acb2032a62c6a2d9', 'ADMIN')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role);

INSERT INTO USER_ACCOUNT (username, password_hash, role)
VALUES ('kubo_tenant', '202ae3ef377b96e2e8ffb2512370ea72723dc1b27c2db41e4fb402c26396bb67', 'TENANT')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role);

INSERT INTO TENANT (user_id, first_name, last_name, contact_number, email, total_balance)
SELECT u.user_id, 'Jamie', 'Rivera', '09171234567', 'jamie@example.com', 2500.00
FROM USER_ACCOUNT u
WHERE u.username = 'kubo_tenant'
  AND NOT EXISTS (SELECT 1 FROM TENANT t WHERE t.user_id = u.user_id);
