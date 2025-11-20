# Fix VS Code running as administrator
# This script removes the "Run as administrator" compatibility flag from VS Code executable

Write-Host "Fixing VS Code administrator elevation..." -ForegroundColor Cyan

# Find VS Code executable
$vscodeUserPath = "$env:LOCALAPPDATA\Programs\Microsoft VS Code\Code.exe"
$vscodeSystemPath = "$env:ProgramFiles\Microsoft VS Code\Code.exe"

$vscodePath = $null
if (Test-Path $vscodeUserPath) {
    $vscodePath = $vscodeUserPath
    Write-Host "Found VS Code at: $vscodePath" -ForegroundColor Green
} elseif (Test-Path $vscodeSystemPath) {
    $vscodePath = $vscodeSystemPath
    Write-Host "Found VS Code at: $vscodePath" -ForegroundColor Green
} else {
    Write-Host "VS Code executable not found!" -ForegroundColor Red
    exit 1
}

# Kill all VS Code processes
Write-Host "`nClosing VS Code processes..." -ForegroundColor Yellow
Get-Process Code -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# Remove compatibility flag from registry
$regPath = "HKCU:\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers"
Write-Host "Checking registry for compatibility flags..." -ForegroundColor Yellow

if (Test-Path $regPath) {
    $value = Get-ItemProperty -Path $regPath -Name $vscodePath -ErrorAction SilentlyContinue
    if ($value) {
        Remove-ItemProperty -Path $regPath -Name $vscodePath -Force
        Write-Host "Removed compatibility flag from registry" -ForegroundColor Green
    } else {
        Write-Host "No registry compatibility flag found" -ForegroundColor Gray
    }
} else {
    Write-Host "No registry compatibility flags exist" -ForegroundColor Gray
}

Write-Host "`nDone! VS Code should no longer run as administrator." -ForegroundColor Green
Write-Host "Launch VS Code normally and try debugging again." -ForegroundColor Cyan
