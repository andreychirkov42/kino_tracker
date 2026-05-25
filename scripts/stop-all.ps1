. "$PSScriptRoot\common.ps1"

$ports = 8080, 5173, 5174, 5175
$processIds = Get-NetTCPConnection -LocalPort $ports -ErrorAction SilentlyContinue |
    Where-Object { $_.State -eq "Listen" } |
    Select-Object -ExpandProperty OwningProcess -Unique

foreach ($procId in $processIds) {
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
}

Write-Host "Stopped backend/frontend ports: 8080, 5173, 5174, 5175" -ForegroundColor Green

Stop-MySqlService
Write-Host "Done." -ForegroundColor Green
