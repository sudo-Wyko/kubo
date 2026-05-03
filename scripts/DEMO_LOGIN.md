# Demo login accounts

After running `seed-demo-accounts.sql` or `.\scripts\seed-demo-accounts.ps1`, sign in with:

| Role   | Username    | Password    |
|--------|-------------|-------------|
| Admin  | `kubo_admin` | `KuboAdmin1!` |
| Tenant | `kubo_tenant` | `KuboTenant1!` |

The tenant row (**Jamie Rivera**) is linked to `kubo_tenant` so the tenant dashboard can load.

**Note:** Login hashes passwords with SHA-256 (`PasswordUtil.hash`) before comparing to `USER_ACCOUNT.password_hash`. After migrating, apply `seed_data.sql` or ensure seeds store hashes as in `seed-demo-accounts.sql`.
