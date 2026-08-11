$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

if (-not (Test-Path $envFile)) {
    Write-Host "Crie o arquivo .env a partir de .env.example:" -ForegroundColor Yellow
    Write-Host "  copy .env.example .env"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $parts = $line -split "=", 2
    if ($parts.Length -ne 2) { return }
    $key = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"').Trim("'")
    Set-Item -Path "Env:$key" -Value $value
}

Write-Host "Variáveis do .env carregadas. Subindo Spring Boot..." -ForegroundColor Green
Set-Location $root
./mvnw spring-boot:run
