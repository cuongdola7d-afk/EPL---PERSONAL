param(
    [Parameter(Mandatory = $true)]
    [string]$Label,
    [string]$SecretsFile = "D:\File Jva\PrismaXI-secrets\aiven-production.env",
    [string]$CaFile = ""
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$pyScript = Join-Path $scriptDir "backup-aiven.py"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
    Write-Output "BACKUP_FAILED python not found in PATH"
    exit 1
}

$argsList = @($pyScript, "--label", $Label, "--secrets-file", $SecretsFile)
if ($CaFile -and $CaFile.Trim().Length -gt 0) {
    $argsList += @("--ca-file", $CaFile)
}

& $python.Source @argsList
exit $LASTEXITCODE
