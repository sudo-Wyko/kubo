-- Obsolete: AFTER INSERT trigger subtracted balance for every status (PENDING/FAILED/VERIFIED).
-- Balance is now updated only in application code when status is or becomes VERIFIED.
-- Safe to run multiple times.

DROP TRIGGER IF EXISTS update_tenant_balance_after_payment;
