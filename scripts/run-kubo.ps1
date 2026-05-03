# Kubo — run the JavaFX app with JDK 17 + local Maven (see RUN_INSTRUCTIONS.md).
# Usage from repo root:  .\scripts\run-kubo.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenVer = "3.9.11"
$mvnCmd = Join-Path $env:LOCALAPPDATA "Apache\Maven\apache-maven-$mavenVer\bin\mvn.cmd"

if (-not (Test-Path $mvnCmd)) {
    Write-Error "Maven not found at $mvnCmd. Run .\scripts\install-prerequisites.ps1 first."
}

$jdkDir = Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-17*" -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Select-Object -First 1

if ($jdkDir) {
    $env:JAVA_HOME = $jdkDir.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Host "Using JAVA_HOME=$env:JAVA_HOME" -ForegroundColor DarkGray
} else {
    Write-Host "WARNING: Temurin JDK 17 not found under Program Files\Eclipse Adoptium. Using whatever java is on PATH." -ForegroundColor Yellow
}

$configPath = Join-Path $repoRoot "config.properties"
if (-not (Test-Path $configPath)) {
    Write-Host "WARNING: config.properties missing. Copy config.properties.example -> config.properties and set db.password." -ForegroundColor Yellow
}

Set-Location $repoRoot
Write-Host "Running mvn javafx:run from $repoRoot ..." -ForegroundColor Cyan
& $mvnCmd javafx:run
