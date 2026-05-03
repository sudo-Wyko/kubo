# Seeds demo USER_ACCOUNT + TENANT rows (see scripts/DEMO_LOGIN.md).
# Requires MySQL credentials: set db.password in config.properties,
# or pass -MysqlPassword, or set env KUBO_MYSQL_PASSWORD.

param(
    [string]$MysqlPassword
)

$ErrorActionPreference = "Stop"

function Read-Props([string]$path) {
    $map = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith("#")) { continue }
        $eq = $t.IndexOf("=")
        if ($eq -lt 1) { continue }
        $k = $t.Substring(0, $eq).Trim()
        $v = $t.Substring($eq + 1).Trim()
        $map[$k] = $v
    }
    $map
}

function Extract-DatabaseName([string]$jdbcUrl) {
    if (-not $jdbcUrl) { return "kubo_db" }
    if ($jdbcUrl -match '/([^/?]+)(\?|$)') {
        return $matches[1]
    }
    return "kubo_db"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $repoRoot "config.properties"
if (-not (Test-Path $configPath)) {
    Write-Error "Missing config.properties at $configPath"
}

$p = Read-Props $configPath
$dbUser = if ($p["db.user"]) { $p["db.user"] } else { "root" }
$dbPass = $MysqlPassword
if (-not $dbPass) { $dbPass = $p["db.password"] }
if (-not $dbPass) { $dbPass = $env:KUBO_MYSQL_PASSWORD }
if (-not $dbPass) {
    Write-Error "MySQL password not set. Add db.password to config.properties, or run:`n  .\scripts\seed-demo-accounts.ps1 -MysqlPassword 'YOUR_PASSWORD'"
}

$dbName = Extract-DatabaseName $p["db.url"]

$mysqlCmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
if (-not $mysqlCmd) {
    $fallback = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (-not (Test-Path $fallback)) {
        Write-Error "mysql.exe not found on PATH or at $fallback"
    }
    $mysqlExe = $fallback
} else {
    $mysqlExe = $mysqlCmd.Source
}

$sqlPath = Join-Path $PSScriptRoot "seed-demo-accounts.sql"
$sqlText = Get-Content -LiteralPath $sqlPath -Raw

Write-Host "Seeding demo accounts into database '$dbName' as user '$dbUser'..." -ForegroundColor Cyan
$sqlText | & $mysqlExe -u $dbUser "--password=$dbPass" $dbName

if ($LASTEXITCODE -ne 0) {
    Write-Error "mysql exited with code $LASTEXITCODE"
}

Write-Host "Done. Login credentials are documented in scripts/DEMO_LOGIN.md" -ForegroundColor Green
