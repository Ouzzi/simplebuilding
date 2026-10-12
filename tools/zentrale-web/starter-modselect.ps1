# Laptop starter add-on: applies the Zentrale mod selection before the integration client starts.
#
# Install (once): copy this file next to starter.ps1 and add two lines to starter.ps1
#   1. near the top:                     . "$PSScriptRoot\starter-modselect.ps1"
#   2. right before the Gradle call that runs :integration:runIntegrationClient (inside the dev folder):
#                                        Invoke-ModSelection -Url $args[0] -Repo $devDir
#      ($args[0] = the simplemods://start?... link the starter receives, $devDir = its dev checkout;
#       use the variable names your starter.ps1 already has.)
#
# The Zentrale start buttons send simplemods://start?ref=wave&off=simplemoney,simplefun (no "off" = all on).
# modselect.py (in the checkout) writes integration/enabled-mods.json and the "Enable Simple X" switches in
# integration/run-fabric-263/config/<super-mod>.json. Errors are logged and the start continues with all mods.

function Invoke-ModSelection {
    param([string]$Url, [string]$Repo)
    $off = ''
    if ($Url -match '[?&]off=([^&]*)') { $off = [Uri]::UnescapeDataString($Matches[1]) }
    if ($off -notmatch '^[a-z0-9_,]*$') { Write-Host "[modselect] ignored invalid off list"; $off = '' }
    $script = Join-Path $Repo 'tools\zentrale-web\modselect.py'
    if (-not (Test-Path $script)) { Write-Host "[modselect] not in this checkout yet, starting with all mods"; return }
    $py = Get-Command py -ErrorAction SilentlyContinue
    if (-not $py) { $py = Get-Command python -ErrorAction SilentlyContinue }
    if (-not $py) { Write-Host "[modselect] no Python found, starting with all mods"; return }
    $pyArgs = @()
    if ($py.Name -eq 'py.exe') { $pyArgs += '-3' }
    $pyArgs += @($script, 'apply', '--repo', $Repo, "--off=$off")  # one token: PowerShell 5.1 drops empty arguments
    $out = & $py.Source @pyArgs 2>&1
    Write-Host "[modselect] $out"
    if ($LASTEXITCODE -ne 0) { Write-Host "[modselect] failed (exit $LASTEXITCODE), starting with the previous selection" }
}
