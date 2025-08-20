@echo off

echo Printing all .xlsx files in current directory...
echo.

for %%f in (*.xlsx) do (
    echo Printing: %%f
    powershell -Command "(New-Object -comObject Excel.Application).Workbooks.Open('%CD%\%%f').PrintOut(); (New-Object -comObject Excel.Application).Quit()"
)

echo.
echo Printing completed.

pause