SHELL := powershell.exe
.SHELLFLAGS := -NoProfile -ExecutionPolicy Bypass -Command

JAVA_HOME ?= C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\jbr
DB_USERNAME ?= root
DB_PASSWORD ?= Admin123!
MYSQL_SERVICE ?= MySQL80
FRONTEND_PORT ?= 5173

.PHONY: help mysql-start mysql-stop backend frontend build build-backend build-frontend install-frontend check stop clean

help:
	@Write-Host ''
	@Write-Host 'Movie Tracker commands:'
	@Write-Host '  make mysql-start      Start MySQL service'
	@Write-Host '  make mysql-stop       Stop MySQL service (admin PowerShell required)'
	@Write-Host '  make backend          Run Spring Boot backend on http://localhost:8080'
	@Write-Host '  make frontend         Run React frontend on http://localhost:$(FRONTEND_PORT)'
	@Write-Host '  make build            Build backend and frontend'
	@Write-Host '  make check            Check MySQL, backend, frontend ports'
	@Write-Host '  make stop             Stop app processes on ports 8080/5173/5174/5175'
	@Write-Host ''

mysql-start:
	@Start-Service -Name '$(MYSQL_SERVICE)'
	@Get-Service -Name '$(MYSQL_SERVICE)' | Select-Object Name,Status

mysql-stop:
	@Stop-Service -Name '$(MYSQL_SERVICE)'
	@Get-Service -Name '$(MYSQL_SERVICE)' | Select-Object Name,Status

backend:
	@Set-Location backend; \
	$$env:JAVA_HOME='$(JAVA_HOME)'; \
	$$env:PATH="$$env:JAVA_HOME\bin;$$env:PATH"; \
	$$env:DB_USERNAME='$(DB_USERNAME)'; \
	$$env:DB_PASSWORD='$(DB_PASSWORD)'; \
	.\mvnw.cmd spring-boot:run

frontend:
	@Set-Location frontend; npm.cmd run dev -- --port $(FRONTEND_PORT)

build: build-backend build-frontend

build-backend:
	@Set-Location backend; \
	$$env:JAVA_HOME='$(JAVA_HOME)'; \
	$$env:PATH="$$env:JAVA_HOME\bin;$$env:PATH"; \
	.\mvnw.cmd -q -DskipTests package

build-frontend:
	@Set-Location frontend; npm.cmd run build

install-frontend:
	@Set-Location frontend; npm.cmd install

check:
	@Test-NetConnection -ComputerName localhost -Port 3306 | Select-Object ComputerName,RemotePort,TcpTestSucceeded
	@Test-NetConnection -ComputerName localhost -Port 8080 | Select-Object ComputerName,RemotePort,TcpTestSucceeded
	@Test-NetConnection -ComputerName localhost -Port $(FRONTEND_PORT) | Select-Object ComputerName,RemotePort,TcpTestSucceeded

stop:
	@$$ports = 8080,5173,5174,5175; \
	$$processIds = Get-NetTCPConnection -LocalPort $$ports -ErrorAction SilentlyContinue | Where-Object { $$_.State -eq 'Listen' } | Select-Object -ExpandProperty OwningProcess -Unique; \
	foreach ($$procId in $$processIds) { Stop-Process -Id $$procId -Force -ErrorAction SilentlyContinue }; \
	Write-Host 'Stopped app processes on ports 8080/5173/5174/5175'

clean:
	@Remove-Item -Recurse -Force backend\target,frontend\dist -ErrorAction SilentlyContinue
