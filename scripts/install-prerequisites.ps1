# Kubo — Windows prerequisite installer (JDK 17 + Apache Maven).
# Run from PowerShell:  .\scripts\install-prerequisites.ps1
# Requires: winget (App Installer / Microsoft Store), PowerShell 5+.

$ErrorActionPreference = "Stop"

Write-Host "== Kubo: installing prerequisites ==" -ForegroundColor Cyan

Write-Host "`n[1/2] Eclipse Temurin JDK 17 (winget)..." -ForegroundColor Yellow
winget install -e --id EclipseAdoptium.Temurin.17.JDK `
    --accept-package-agreements --accept-source-agreements --silent

Write-Host "`n[2/2] Apache Maven (download to LOCALAPPDATA)..." -ForegroundColor Yellow
$mavenVer = "3.9.11"
$mavenRoot = Join-Path $env:LOCALAPPDATA "Apache\Maven"
$mvnHome = Join-Path $mavenRoot "apache-maven-$mavenVer"
$mvnCmd = Join-Path $mvnHome "bin\mvn.cmd"

if (-not (Test-Path $mvnCmd)) {
    $zip = Join-Path $env:TEMP "apache-maven-$mavenVer-bin.zip"
    $uri = "https://archive.apache.org/dist/maven/maven-3/$mavenVer/binaries/apache-maven-$mavenVer-bin.zip"
    Write-Host "Downloading $uri"
    Invoke-WebRequest -Uri $uri -OutFile $zip -UseBasicParsing
    New-Item -ItemType Directory -Force -Path $mavenRoot | Out-Null
    Expand-Archive -Path $zip -DestinationPath $mavenRoot -Force
}

if (-not (Test-Path $mvnCmd)) {
    throw "Maven install failed: $mvnCmd not found."
}

Write-Host "`nMaven:" -ForegroundColor Green
& $mvnCmd -version

Write-Host "`nDone. Next:" -ForegroundColor Cyan
Write-Host "  1. Install/start MySQL (see RUN_INSTRUCTIONS.md)" 
Write-Host "  2. Copy config.properties.example -> config.properties and edit credentials"
Write-Host "  3. Import database/schema.sql"
Write-Host "  4. Run: .\scripts\run-kubo.ps1"
