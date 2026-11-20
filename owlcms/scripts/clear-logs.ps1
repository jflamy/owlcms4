# Deletes the owlcms file logs (logs/*.log and logs/audit/*.log) before a launch.
# Windows counterpart of the bash command in the "clear owlcms file logs" task.
$logs = Join-Path (Split-Path -Parent $PSScriptRoot) 'logs'
foreach ($dir in @($logs, (Join-Path $logs 'audit'))) {
    Get-ChildItem -LiteralPath $dir -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension -eq '.log' } |
        Remove-Item -Force -ErrorAction SilentlyContinue
}
exit 0
