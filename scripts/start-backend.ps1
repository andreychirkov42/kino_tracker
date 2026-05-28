. "$PSScriptRoot\common.ps1"

$ErrorActionPreference = "Stop"

Ensure-MySqlRunning

$env:JAVA_HOME = Resolve-JavaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { $DefaultDbUsername }
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $DefaultDbPassword }

Set-Location $BackendDir

$jar = Get-ChildItem -Path "target" -Filter "movie-tracker-backend-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

$needsBuild = -not $jar
if (-not $needsBuild) {
    $newestSource = Get-ChildItem -Path "src", "pom.xml" -Recurse -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($newestSource -and $newestSource.LastWriteTime -gt $jar.LastWriteTime) {
        $needsBuild = $true
    }
}

if ($needsBuild) {
    Write-Host "Building backend jar..." -ForegroundColor Yellow
    & .\mvnw.cmd -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
    $jar = Get-ChildItem -Path "target" -Filter "movie-tracker-backend-*.jar" |
        Where-Object { $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

Write-Host "Backend: http://localhost:8080" -ForegroundColor Cyan
Write-Host "DB user: $env:DB_USERNAME" -ForegroundColor DarkGray
Write-Host "Jar: $($jar.FullName)" -ForegroundColor DarkGray

& "$env:JAVA_HOME\bin\java.exe" --enable-native-access=ALL-UNNAMED -jar $jar.FullName
