$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaExe = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe'
$launchCfg = Join-Path $projectRoot '.gradle\loom-cache\launch.cfg'
$argFile = Join-Path $projectRoot 'build\loom-cache\argFiles\runClient'

if (!(Test-Path $javaExe)) {
    throw "Java executable not found: $javaExe"
}
if (!(Test-Path $launchCfg)) {
    throw "Loom launch config not found: $launchCfg"
}
if (!(Test-Path $argFile)) {
    throw "Loom runClient arg file not found: $argFile"
}

# Stop stale dev client processes that can lock remapped mod jars on Windows.
Get-CimInstance Win32_Process |
    Where-Object {
        ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and
        $_.CommandLine -match [regex]::Escape($projectRoot) -and
        $_.CommandLine -match 'fabric\.dli\.main=net\.fabricmc\.loader\.impl\.launch\.knot\.KnotClient'
    } |
    ForEach-Object {
        try {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction Stop
            Write-Host "Stopped stale process PID $($_.ProcessId)"
        } catch {
            Write-Warning "Could not stop PID $($_.ProcessId): $($_.Exception.Message)"
        }
    }

$encodedRoot = $projectRoot -replace ' ', '@@0020'
$dliConfig = "-Dfabric.dli.config=$encodedRoot\.gradle\loom-cache\launch.cfg"

$javaArgs = @(
    $dliConfig
    '-Dfabric.dli.env=client'
    '-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient'
    "@$argFile"
    '-Dfile.encoding=UTF-8'
    '-Duser.country=DE'
    '-Duser.language=de'
    '-Duser.variant'
    'net.fabricmc.devlaunchinjector.Main'
)

$runDir = Join-Path $projectRoot 'run'
if (!(Test-Path $runDir)) { New-Item -ItemType Directory -Path $runDir | Out-Null }
Push-Location $runDir
try {
    & $javaExe @javaArgs
} finally {
    Pop-Location
}
