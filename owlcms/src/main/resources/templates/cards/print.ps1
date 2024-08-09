# Get the current folder path
$folderPath = Get-Location

# Open Excel application
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false

# Get all .xls and .xlsx files in the folder, sorted alphabetically
$files = Get-ChildItem -Path $folderPath -Filter "*.xls*" | Sort-Object Name

# Print each file
foreach ($file in $files) {
    $workbook = $excel.Workbooks.Open($file.FullName)
    $workbook.PrintOut()
    $workbook.Close($false)
}

# Quit Excel application
$excel.Quit()
