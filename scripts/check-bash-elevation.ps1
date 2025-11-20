# Check if Git Bash shortcuts are configured to run as administrator

Write-Host "Checking Git Bash shortcuts..." -ForegroundColor Cyan

$locations = @(
    "$env:ProgramData\Microsoft\Windows\Start Menu\Programs\Git\*.lnk",
    "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\Git\*.lnk",
    "$env:USERPROFILE\Desktop\*.lnk",
    "$env:USERPROFILE\AppData\Roaming\Microsoft\Internet Explorer\Quick Launch\User Pinned\TaskBar\*.lnk"
)

$found = $false

foreach ($pattern in $locations) {
    $shortcuts = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | 
                 Where-Object { $_.Name -like "*bash*" -or $_.Name -like "*Git*" }
    
    foreach ($shortcut in $shortcuts) {
        $found = $true
        Write-Host "`nShortcut: $($shortcut.Name)" -ForegroundColor Yellow
        Write-Host "Location: $($shortcut.FullName)" -ForegroundColor Gray
        
        $bytes = [System.IO.File]::ReadAllBytes($shortcut.FullName)
        
        if ($bytes[21] -band 0x20) {
            Write-Host "Status: RUNS AS ADMINISTRATOR" -ForegroundColor Red
            Write-Host "This is why VS Code runs elevated!" -ForegroundColor Red
        } else {
            Write-Host "Status: Normal (not elevated)" -ForegroundColor Green
        }
    }
}

if (-not $found) {
    Write-Host "`nNo Git Bash shortcuts found." -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "If any shortcuts are elevated:" -ForegroundColor White
Write-Host "  Right-click shortcut → Properties" -ForegroundColor Gray
Write-Host "  → Advanced → Uncheck 'Run as administrator'" -ForegroundColor Gray
Write-Host "========================================`n" -ForegroundColor Cyan
