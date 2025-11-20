#!/bin/bash
# Launch VS Code as a normal user (non-elevated) even from an elevated shell

WORKSPACE="c:/Dev/git/owlcms_v23stable/owlcms_v23master/jfl.code-workspace"

# Use PowerShell to launch Code.exe as current user without elevation
powershell.exe -Command "Start-Process -FilePath 'code' -ArgumentList '--new-window','$WORKSPACE' -Verb RunAsUser"

echo "Launched VS Code non-elevated with workspace"
