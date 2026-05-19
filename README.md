# Kubo Property Management System

Kubo is a JavaFX-based boarding house and property management system. It helps manage tenants, rooms, leases, and payments through two main interfaces: an **Admin** dashboard for landlords/managers and a **Tenant** portal for renters.

## Features

### Admin Side
- **Manage Tenants:** Add new tenants or soft-delete them to keep records.
- **Manage Rooms:** Track room capacity, pricing, and current occupancy.
- **Leases:** View active, expired, and terminated leases.
- **Payments:** Accept and verify tenant payments.
- **Maintenance & Documents:** Review maintenance requests and uploaded tenant documents.
- **Activity Logs:** Tracks payments, lease updates, and related events via database triggers.

### Tenant Side
- **Dashboard:** View balance, lease details, and announcements.
- **Payments:** Submit payment records for admin verification.
- **Maintenance:** Report issues for admin follow-up.
- **Rooms:** Browse available rooms and pricing.

## Built With

| Layer | Technology |
|-------|------------|
| UI | JavaFX 13 (FXML + CSS) |
| Language | Java 11 (modules) |
| Database | MySQL 8+ |
| Build | Maven |
| Standalone packaging | `jpackage` via `jpackage-maven-plugin` |

## Prerequisites

1. **JDK 11+** (JDK 14+ required for `jpackage` standalone builds on Windows)
2. **Apache Maven 3.6+**
3. **MySQL Server** running locally (or reachable on your network)
4. **WiX Toolset 3.x** (Windows only, for MSI/installer-style `jpackage` output if you extend beyond `APP_IMAGE`)

---

## Configuration & Secure Local Storage

Kubo no longer reads or writes `config.properties` in the application install or project root directory. Database credentials and connection settings are stored in the **per-user application data directory**, which aligns with standard Windows deployment security practices (writable user profile, not the protected Program Files area).

### Where settings are stored

| Platform | Config file path |
|----------|------------------|
| **Windows** | `%APPDATA%\Kubo\config.properties` (e.g. `C:\Users\<You>\AppData\Roaming\Kubo\config.properties`) |
| **Linux / macOS** | `~/.kubo/config.properties` |

On first run, if no configuration exists, Kubo shows a **Database Setup** dialog. Saved settings are written automatically; the parent folder is created if it does not exist.

### Manual configuration (optional)

You may create or edit the file directly:

```properties
db.host=jdbc:mysql://localhost:3306
db.name=kubo_db
db.user=your_username_here
db.password=your_password_here
```

> **Note:** A legacy `config.properties` in the project or install folder is **not** used. Copy values into the AppData path above if you are migrating from an older build.

---

## Running the Application

### First-run database behavior

Kubo is designed to be largely plug-and-play: you do not need to run SQL scripts manually. On startup it can create the `kubo_db` database, tables, and default accounts automatically.

**Tip:** For a clean first-time schema, ensure `kubo_db` does not already exist (or drop it: `DROP DATABASE kubo_db;`).

### Default login

| Role | Username | Password |
|------|----------|----------|
| Super Admin | `admin` | `admin123` |

Tenants can register from the sign-up screen.

---

## Development & Execution

### Option A — Maven (recommended)

From the project root:

```bash
# Compile
mvn clean compile

# Run with JavaFX Maven plugin (development)
mvn javafx:run
```

One-shot compile and run:

```bash
mvn clean compile javafx:run
```

### Option B — IDE

Run the `main` method in `com.teamroy.Launcher` (or `com.teamroy.App`). Ensure the module path includes JavaFX and MySQL dependencies (Maven/IDE handles this automatically).

### Option C — Manual `javac` / `java` (from scratch)

Use Maven once to download dependencies, then compile and launch without the JavaFX plugin.

**Step 1 — Copy dependencies to a local `lib` folder:**

```bash
mvn -q dependency:copy-dependencies -DoutputDirectory=target/lib
```

**Step 2 — Compile all sources (Windows PowerShell):**

```powershell
$src = Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { $_.FullName }
javac -d target\classes `
  --module-path target/lib `
  --add-modules javafx.controls,javafx.fxml `
  -sourcepath src\main\java `
  src\main\java\module-info.java `
  $src
```

**Step 2 — Compile (Linux / macOS bash):**

```bash
find src/main/java -name "*.java" > sources.txt
javac -d target/classes \
  --module-path target/lib \
  --add-modules javafx.controls,javafx.fxml \
  -sourcepath src/main/java \
  @sources.txt
```

**Step 3 — Copy resources:**

```bash
# Windows (PowerShell)
Copy-Item -Recurse -Force src\main\resources\* target\classes\

# Linux / macOS
cp -r src/main/resources/* target/classes/
```

**Step 4 — Run:**

```bash
java --module-path "target/lib;target/classes" ^
  --add-modules javafx.controls,javafx.fxml ^
  -m com.teamroy/com.teamroy.Launcher
```

On Linux/macOS, use `:` instead of `;` in the module path:

```bash
java --module-path "target/lib:target/classes" \
  --add-modules javafx.controls,javafx.fxml \
  -m com.teamroy/com.teamroy.Launcher
```

---

## Standalone Deployment (Windows)

Kubo ships with a **standalone deployment pipeline** using Maven Shade (fat JAR) and `jpackage` (native application image).

### Build steps

```bash
# 1. Produce shaded executable JAR
mvn clean package

# 2. Generate standalone app image (output: target/dist/Kubo/)
mvn jpackage:jpackage
```

The `jpackage-maven-plugin` is configured in `pom.xml` with:

- **Name:** `Kubo`
- **Type:** `APP_IMAGE` (portable folder under `target/dist`)
- **Main JAR:** `kubo-1.0.0.jar`
- **Main class:** `com.teamroy.Launcher`
- **Icon:** `src/main/resources/kubo_icon.ico`

### Distributing to end users

1. Copy the entire `target/dist/Kubo/` folder (or zip it) to the target machine.
2. Run `Kubo.exe` from that folder (Windows).
3. Ensure MySQL is installed and reachable; complete the first-run database setup dialog if prompted.
4. User-specific `config.properties` is created under `%APPDATA%\Kubo\` — not beside the executable — so installs under `Program Files` remain secure and updatable.

To produce an MSI or EXE installer instead of a folder image, change `<type>APP_IMAGE</type>` to `<type>msi</type>` or `<type>exe</type>` in `pom.xml` and ensure WiX (Windows) prerequisites are installed.

### Run the fat JAR without jpackage

```bash
mvn clean package
java -jar target/kubo-1.0.0.jar
```

> The shaded JAR still requires a JRE and JavaFX modules on the module path unless you use the `jpackage` image, which bundles a runtime.

---

## Project Layout (high level)

```
kubo/
├── pom.xml                          # Maven build, shade, jpackage
├── src/main/java/com/teamroy/       # Application entry (Launcher, App, DatabaseConfig)
├── src/main/resources/              # FXML, CSS, icons
└── README.md
```

## Troubleshooting

| Issue | Suggestion |
|-------|------------|
| Cannot connect to MySQL | Verify MySQL service is running; check `%APPDATA%\Kubo\config.properties` |
| Setup dialog keeps appearing | Ensure `db.host`, `db.name`, and `db.user` are set in the AppData config file |
| JavaFX runtime errors (manual run) | Confirm `--module-path` includes JavaFX JARs and `--add-modules javafx.controls,javafx.fxml` |
| `jpackage` fails on Windows | Use JDK 14+, install WiX 3.x for installer types, run `mvn package` before `jpackage:jpackage` |

## License

See repository ownership and course/project guidelines as applicable.
