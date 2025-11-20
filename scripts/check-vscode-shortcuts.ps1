# Check and fix all VS Code shortcuts to not run as administrator

Write-Host "Checking VS Code shortcuts for admin elevation..." -ForegroundColor Cyan

# Define common shortcut locations
$locations = @(
    "$env:USERPROFILE\Desktop\*.lnk",
    "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\*.lnk",
    "$env:PUBLIC\Desktop\*.lnk",
    "$env:ProgramData\Microsoft\Windows\Start Menu\Programs\*.lnk"
)

$shell = New-Object -ComObject WScript.Shell
$foundVSCode = $false

foreach ($location in $locations) {
    $shortcuts = Get-ChildItem -Path $location -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "*Code*" -or $_.Name -like "*VSCode*" }
    
    foreach ($shortcut in $shortcuts) {
        $link = $shell.CreateShortcut($shortcut.FullName)
        
        # Check if it points to VS Code
        if ($link.TargetPath -like "*Code.exe*") {
            $foundVSCode = $true
            Write-Host "`nFound VS Code shortcut: $($shortcut.FullName)" -ForegroundColor Yellow
            Write-Host "  Target: $($link.TargetPath)" -ForegroundColor Gray
            
            # Read the shortcut file bytes to check for admin flag
            $bytes = [System.IO.File]::ReadAllBytes($shortcut.FullName)
            
            # Byte 21 contains flags - bit 5 (0x20) is "Run as administrator"
            if ($bytes[21] -band 0x20) {
                Write-Host "  Status: RUNS AS ADMINISTRATOR" -ForegroundColor Red
                
                # Remove the admin flag
                $bytes[21] = $bytes[21] -band (-bnot 0x20)
                [System.IO.File]::WriteAllBytes($shortcut.FullName, $bytes)
                Write-Host "  Fixed: Removed administrator flag" -ForegroundColor Green
            } else {
                Write-Host "  Status: Normal (not elevated)" -ForegroundColor Green
            }
        }
    }
}

if (-not $foundVSCode) {
    Write-Host "`nNo VS Code shortcuts found in common locations" -ForegroundColor Yellow
    Write-Host "Manually check your desktop icon properties:" -ForegroundColor Cyan
    Write-Host "  Right-click → Properties → Advanced → Uncheck 'Run as administrator'" -ForegroundColor Gray
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Next steps:" -ForegroundColor White
Write-Host "1. Close VS Code completely" -ForegroundColor Gray
Write-Host "2. Launch VS Code from the desktop icon" -ForegroundColor Gray
Write-Host "3. The admin warning should be gone" -ForegroundColor Gray
Write-Host "========================================`n" -ForegroundColor Cyan
