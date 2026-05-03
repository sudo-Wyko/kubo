-- Post-migration data fixes for Kubo (run manually against your kubo_db after updating the app).
-- 1) Remove legacy trigger that adjusts balance on every payment insert regardless of status.
-- 2) Store SHA-256 hashes for password '123'.

USE kubo_db;

DROP TRIGGER IF EXISTS update_tenant_balance_after_payment;

UPDATE USER_ACCOUNT
SET password_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
WHERE username IN ('admin', 'tenant');

-- Demo accounts kubo_* use SHA-256 hashes in scripts/seed-demo-accounts.sql (re-run seed after hashing change).
