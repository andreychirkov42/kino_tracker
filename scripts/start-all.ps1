. "$PSScriptRoot\common.ps1"

$ErrorActionPreference = "Stop"

Ensure-MySqlRunning

Write-Host "Starting backend in a new PowerShell window..." -ForegroundColor Cyan
Start-Process powershell.exe -ArgumentList "-NoExit -NoProfile -ExecutionPolicy Bypass -File `"$PSScriptRoot\start-backend.ps1`""

Write-Host "Waiting for backend port 8080..." -ForegroundColor Yellow
if (-not (Wait-LocalPort -Port 8080 -TimeoutSeconds 45)) {
    Write-Host "Backend did not open port 8080 yet. Check the backend window." -ForegroundColor Yellow
}

Write-Host "Starting frontend in a new PowerShell window..." -ForegroundColor Cyan
Start-Process powershell.exe -ArgumentList "-NoExit -NoProfile -ExecutionPolicy Bypass -File `"$PSScriptRoot\start-frontend.ps1`""

Write-Host ""
Write-Host "Open site:" -ForegroundColor Green
Write-Host "http://localhost:$DefaultFrontendPort" -ForegroundColor Green
Write-Host ""
Write-Host "Login:"
Write-Host "student / 123456"
Write-Host "admin   / admin123"
