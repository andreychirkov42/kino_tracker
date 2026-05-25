$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"
$MysqlService = "MySQL80"
$DefaultDbUsername = "root"
$DefaultDbPassword = ""
$DefaultFrontendPort = 5173

function Resolve-JavaHome {
    $configured = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\jbr"
    if (Test-Path (Join-Path $configured "bin\java.exe")) {
        return $configured
    }

    $candidate = Get-ChildItem "C:\Program Files\JetBrains" -Directory -Filter "IntelliJ IDEA*" -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName "jbr" } |
        Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } |
        Select-Object -First 1

    if ($candidate) {
        return $candidate
    }

    throw "Java not found. Install JDK or update scripts/common.ps1 JAVA_HOME path."
}

function Wait-LocalPort {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($connection) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Ensure-MySqlRunning {
    $mysqlPort = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
    if ($mysqlPort) {
        Write-Host "MySQL is already running on port 3306." -ForegroundColor Green
        return
    }

    $service = Get-Service -Name $MysqlService -ErrorAction SilentlyContinue
    if (-not $service) {
        throw "MySQL service '$MysqlService' not found and port 3306 is closed. Start MySQL in XAMPP manually."
    }

    if ($service.Status -eq "Running") {
        Write-Host "MySQL is already running." -ForegroundColor Green
        return
    }

    Write-Host "Starting MySQL service..." -ForegroundColor Yellow
    try {
        Start-Service -Name $MysqlService -ErrorAction Stop
    } catch {
        Write-Host "Need administrator rights. Opening UAC window for MySQL start..." -ForegroundColor Yellow
        Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -Command `"Start-Service -Name '$MysqlService'`""
    }

    $deadline = (Get-Date).AddSeconds(30)
    do {
        Start-Sleep -Seconds 2
        $service = Get-Service -Name $MysqlService -ErrorAction SilentlyContinue
    } while ($service.Status -ne "Running" -and (Get-Date) -lt $deadline)

    if ($service.Status -ne "Running") {
        throw "MySQL did not start. Start it manually: net start $MysqlService"
    }

    Write-Host "MySQL started." -ForegroundColor Green
}

function Stop-MySqlService {
    $service = Get-Service -Name $MysqlService -ErrorAction SilentlyContinue
    if (-not $service) {
        Write-Host "MySQL service '$MysqlService' not found." -ForegroundColor Yellow
        $mysqlProcessIds = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $mysqlProcessIds) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
        return
    }

    if ($service.Status -ne "Running") {
        $mysqlProcessIds = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $mysqlProcessIds) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
        Write-Host "MySQL service is stopped. Closed MySQL process on port 3306 if it existed." -ForegroundColor Green
        return
    }

    Write-Host "Stopping MySQL service..." -ForegroundColor Yellow
    try {
        Stop-Service -Name $MysqlService -ErrorAction Stop
    } catch {
        Write-Host "Need administrator rights. Opening UAC window for MySQL stop..." -ForegroundColor Yellow
        Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -Command `"Stop-Service -Name '$MysqlService'`""
    }
}
