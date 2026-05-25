. "$PSScriptRoot\common.ps1"

$ErrorActionPreference = "Stop"

Ensure-MySqlRunning

$env:JAVA_HOME = Resolve-JavaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { $DefaultDbUsername }
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $DefaultDbPassword }

Write-Host "Backend: http://localhost:8080" -ForegroundColor Cyan
Write-Host "DB user: $env:DB_USERNAME" -ForegroundColor DarkGray

Set-Location $BackendDir
.\mvnw.cmd spring-boot:run
