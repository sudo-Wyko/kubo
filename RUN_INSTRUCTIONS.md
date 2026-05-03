# Kubo Property Management — Install & Run (Windows)

This guide lists everything you need and how to start the desktop app.

---

## What you need

| Requirement | Purpose |
|-------------|---------|
| **Windows 10/11** | Desktop OS |
| **winget** | Installs JDK (Microsoft App Installer). Open Store → install “App Installer” if `winget` is missing. |
| **JDK 17** | Builds/runs the project (matches README; `pom.xml` targets Java 11+). |
| **Apache Maven** | Downloads dependencies and runs `javafx:run`. |
| **MySQL Server** | Database for tenants, payments, maintenance, etc. |
| **Optional: Cursor / VS Code + Extension Pack for Java** | Editing and debugging |

Maven does **not** need to be on your PATH if you use the scripts below (Maven is installed under your user profile).

---

## One-time automated setup (recommended)

Open **PowerShell**, go to the project folder (where `pom.xml` is), then:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned -Force
.\scripts\install-prerequisites.ps1
```

This script:

1. Installs **Eclipse Temurin JDK 17** via `winget`.
2. Downloads **Apache Maven 3.9.11** into `%LOCALAPPDATA%\Apache\Maven\apache-maven-3.9.11`.

If `winget` fails (policy/network), install JDK 17 manually from [Adoptium](https://adoptium.net/) and Maven from [Apache Maven](https://maven.apache.org/download.cgi), then add `mvn` and `java` to your PATH.

---

## MySQL — install or verify

### If MySQL is not installed yet

```powershell
winget install -e --id Oracle.MySQL --accept-package-agreements --accept-source-agreements
```

Follow the installer’s prompts (note the **root password** you choose).

### Check that the server is running

```powershell
Get-Service -Name "*mysql*"
```

If the service exists but is stopped (Administrator PowerShell):

```powershell
net start MySQL80
```

(Service name may be `MySQL80`, `MySQL57`, etc.)

---

## Database schema

1. Open MySQL (Workbench, CLI, or HeidiSQL).
2. Run the SQL in **`database/schema.sql`** from this repo (creates tables/database as defined there).

Typical CLI example (adjust password):

```powershell
mysql -u root -p < database\schema.sql
```

Ensure the database name in **`config.properties`** matches your schema (default in this project is usually **`kubo_db`**).

---

## Demo accounts (optional)

If MySQL reports **`Unknown database 'kubo_db'`**, apply the bundled schema bootstrap first:

```powershell
mysql -u root -p < database/init_kubo_db.sql
```

(Or paste/run `database/init_kubo_db.sql` in Workbench.)

Seed script creates:

| Role | Username | Password |
|------|-----------|----------|
| Admin | `kubo_admin` | `KuboAdmin1!` |
| Tenant | `kubo_tenant` | `KuboTenant1!` |

Requires **`db.password`** (or `-MysqlPassword`). From project root:

```powershell
.\scripts\seed-demo-accounts.ps1
```

See **`scripts/DEMO_LOGIN.md`** for the same table.

---

## Configuration (`config.properties`)

1. Copy **`config.properties.example`** → **`config.properties`** (same folder as `pom.xml`).
2. Edit:

```properties
db.url=jdbc:mysql://localhost:3306/kubo_db
db.user=root
db.password=YOUR_ACTUAL_PASSWORD
```

**Never commit `config.properties`** (it is listed in `.gitignore`).

---

## Run the application

### Option A — helper script (uses JDK 17 + bundled Maven path)

From the project root:

```powershell
.\scripts\run-kubo.ps1
```

### Option B — Maven on PATH

After JDK + Maven are installed and on PATH:

```powershell
cd path\to\kubo-system-dao-implementation
mvn javafx:run
```

Clean build:

```powershell
mvn clean javafx:run
```

### Option C — IDE

Import as a **Maven** project, then execute the **`javafx:run`** goal from the Maven tool window (main class is **`com.teamroy.App`** per `pom.xml`).

---

## Troubleshooting

| Problem | What to try |
|---------|--------------|
| **`Could not find config.properties`** | Create `config.properties` next to `pom.xml` (copy from `config.properties.example`). |
| **`Access denied … using password: NO`** | Set `db.password` in `config.properties`. |
| **`Communications link failure` / connection refused** | Start MySQL service; check host/port in `db.url`. |
| **`Unknown database 'kubo_db'`** | Create the DB or run `database/schema.sql`. |
| **`mvn` not recognized** | Run `install-prerequisites.ps1` or use `.\scripts\run-kubo.ps1`. |
| **Wrong Java version picked up** | Set `JAVA_HOME` to JDK 17, e.g. `C:\Program Files\Eclipse Adoptium\jdk-17.*`, put `%JAVA_HOME%\bin` first on PATH. |
| **JavaFX / Unsafe warnings** | Often harmless with JDK 21+ vs JavaFX 13 in `pom.xml`; aligning JavaFX version with your JDK reduces warnings. |

---

## Quick checklist

1. Run `.\scripts\install-prerequisites.ps1`
2. Install/start **MySQL**, apply **`database/schema.sql`**
3. **`config.properties`** with correct **`db.password`**
4. **`.\scripts\run-kubo.ps1`** or **`mvn javafx:run`**

---

## Official README

See **`README.md`** for repository conventions (branches, etc.).
