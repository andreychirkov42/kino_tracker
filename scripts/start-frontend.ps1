. "$PSScriptRoot\common.ps1"

$ErrorActionPreference = "Stop"

Set-Location $FrontendDir

if (-not (Test-Path "node_modules")) {
    Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
    npm.cmd install
}

Write-Host "Frontend: http://localhost:$DefaultFrontendPort" -ForegroundColor Cyan
npm.cmd run dev -- --port $DefaultFrontendPort
