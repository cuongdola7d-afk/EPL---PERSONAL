param(
    [string]$Backup = ""
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$pyScript = Join-Path $scriptDir "h2-from-latest-backup.py"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
    Write-Output "H2_FAILED python not found in PATH"
    exit 1
}

$argsList = @($pyScript)
if ($Backup -and $Backup.Trim().Length -gt 0) {
    $argsList += @("--backup", $Backup)
}

& $python.Source @argsList
exit $LASTEXITCODE
