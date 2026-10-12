# SimpleMods: set up a Windows PC once for the "Laptop-Start" buttons of the Zentrale (simplemods:// links).
#   powershell -NoProfile -ExecutionPolicy Bypass -File install.ps1 [-Zentrale https://<zentrale>] [-DevDir <folder>]
# Installs with winget what is missing (Git, Eclipse Temurin JDK 25, Python 3 + Pillow/numpy for the texture build,
# cloudflared for the login to the Zentrale), copies starter.ps1 to ~\.simplemods, registers simplemods:// for this
# user (HKCU, no admin), clones the public repo into the dev folder and fetches the working state once.
# Safe to run again (repairs / updates). No secrets: the Zentrale login runs in the browser (cloudflared access login).
# Keep this file ASCII (PowerShell 5.1 reads it as ANSI).
param([string]$Zentrale = '', [string]$DevDir = '')

$ErrorActionPreference = 'Stop'
$Home2 = Join-Path $env:USERPROFILE '.simplemods'
$LogFile = Join-Path $Home2 'install.log'

function Write-Step([string]$Text) {
    Write-Host ''; Write-Host "==> $Text" -ForegroundColor Cyan
    try { Add-Content -LiteralPath $LogFile -Value ('{0:yyyy-MM-dd HH:mm:ss} {1}' -f (Get-Date), $Text) -Encoding UTF8 } catch { }
}
function Write-Note([string]$Text) {
    Write-Host "    $Text"
    try { Add-Content -LiteralPath $LogFile -Value "    $Text" -Encoding UTF8 } catch { }
}
function Show-Popup([string]$Text, [int]$Icon = 16) {
    try { [void](New-Object -ComObject WScript.Shell).Popup($Text, 0, 'SimpleMods-Installer', $Icon) } catch { Write-Host $Text }
}
function Update-Path {
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')
}
function Find-Exe([string[]]$Names, [string[]]$Candidates) {
    foreach ($n in $Names) { $c = Get-Command $n -ErrorAction SilentlyContinue | Select-Object -First 1; if ($c) { return $c.Source } }
    foreach ($c in $Candidates) { if ($c -and (Test-Path -LiteralPath $c)) { return $c } }
    return ''
}
function Invoke-Quiet([string]$Exe, [string[]]$Arguments) {
    $old = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    try { $out = & $Exe @Arguments 2>&1 | ForEach-Object { "$_" } } finally { $ErrorActionPreference = $old }
    $script:LastExit = $LASTEXITCODE
    return ,@($out)
}

# winget: user scope where the package supports it (no admin); otherwise the package's own installer asks once (UAC).
function Install-WingetPackage([string]$Id, [string]$What, [switch]$UserScope) {
    Write-Note "installiere $What ($Id) ..."
    $a = @('install', '--id', $Id, '--exact', '--silent', '--accept-package-agreements', '--accept-source-agreements', '--disable-interactivity')
    if ($UserScope) { $a += @('--scope', 'user') }
    $old = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    try { & winget @a 2>&1 | ForEach-Object { Write-Host "      $_" } } finally { $ErrorActionPreference = $old }
    $code = $LASTEXITCODE
    if ($code -ne 0 -and $UserScope) {
        Write-Note 'Benutzer-Installation nicht moeglich, versuche systemweit (Windows fragt evtl. nach Admin-Rechten) ...'
        $a = $a[0..($a.Count - 3)]
        $ErrorActionPreference = 'Continue'
        try { & winget @a 2>&1 | ForEach-Object { Write-Host "      $_" } } finally { $ErrorActionPreference = $old }
        $code = $LASTEXITCODE
    }
    Update-Path
    # -1978335189 = already installed / no newer version
    if ($code -ne 0 -and $code -ne -1978335189) { Write-Note "winget meldet Exit $code fuer $Id" }
}

function Find-JavaHome {
    foreach ($r in @("$env:ProgramFiles\Eclipse Adoptium", "$env:LOCALAPPDATA\Programs\Eclipse Adoptium", "$env:ProgramFiles\Java")) {
        if (-not (Test-Path -LiteralPath $r)) { continue }
        $d = Get-ChildItem -LiteralPath $r -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($d -and (Test-Path -LiteralPath (Join-Path $d.FullName 'bin\java.exe'))) { return $d.FullName }
    }
    return ''
}

# Python executable that has Pillow + numpy (the build calls it directly; "py -3" is resolved to the real path).
function Find-Python([switch]$Any) {
    $cands = @()
    $py = Find-Exe @('py') @("$env:LOCALAPPDATA\Programs\Python\Launcher\py.exe", "$env:SystemRoot\py.exe")
    if ($py) {
        $o = Invoke-Quiet $py @('-3', '-c', 'import sys; print(sys.executable)')
        if ($script:LastExit -eq 0) { $cands += ($o -join '').Trim() }
    }
    foreach ($n in @('python', 'python3')) {
        foreach ($c in @(Get-Command $n -All -ErrorAction SilentlyContinue)) {
            if ($c.Source -notmatch 'WindowsApps') { $cands += $c.Source }  # skip the Microsoft Store alias
        }
    }
    $cands += @(Get-ChildItem "$env:LOCALAPPDATA\Programs\Python\Python3*\python.exe" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    foreach ($c in ($cands | Where-Object { $_ } | Select-Object -Unique)) {
        if ($Any) { return $c }
        [void](Invoke-Quiet $c @('-c', 'import PIL, numpy'))
        if ($script:LastExit -eq 0) { return $c }
    }
    return ''
}

function Register-Protocol([string]$Starter) {
    $ps = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    $key = 'HKCU:\Software\Classes\simplemods'
    New-Item -Path "$key\shell\open\command" -Force | Out-Null
    Set-Item -Path $key -Value 'URL:SimpleMods Starter'
    New-ItemProperty -Path $key -Name 'URL Protocol' -Value '' -PropertyType String -Force | Out-Null
    Set-Item -Path "$key\shell\open\command" -Value ('"{0}" -NoProfile -ExecutionPolicy Bypass -File "{1}" "%1"' -f $ps, $Starter)
}

# <Zentrale>/<Path> with the Cloudflare Access token of cloudflared (browser login when missing or expired).
# The starter has the same logic for the working-state bundle.
function Get-ZentraleFile([string]$Url, [string]$Cf, [string]$Path, [string]$OutFile) {
    for ($try = 0; $try -lt 2; $try++) {
        $tok = ''
        if ($try -eq 0) { $tok = ((Invoke-Quiet $Cf @('access', 'token', "-app=$Url")) -join '').Trim() }
        if ($tok -notmatch '^ey[\w-]+\.[\w-]+\.[\w-]+$') {
            Write-Note 'Anmeldung an der Zentrale im Browser (Cloudflare Access) - bitte dort bestaetigen ...'
            [void](Invoke-Quiet $Cf @('access', 'login', $Url))
            $tok = ((Invoke-Quiet $Cf @('access', 'token', "-app=$Url")) -join '').Trim()
        }
        try {
            $r = Invoke-WebRequest -Uri "$Url/$Path" -Headers @{ 'cf-access-token' = $tok } -OutFile $OutFile -PassThru -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 120
            if ([int]$r.StatusCode -eq 200 -and [string]$r.Headers['Content-Type'] -notmatch 'text/html') { return }
        } catch { Write-Note "Download $Url/${Path}: $($_.Exception.Message)" }
    }
    throw "Konnte $Url/$Path nicht laden (Anmeldung an der Zentrale?)."
}

function Main([string]$Zentrale, [string]$DevDir) {
    New-Item -ItemType Directory -Force -Path $Home2 | Out-Null
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    Write-Step 'SimpleMods einrichten'
    $cfgFile = Join-Path $Home2 'config.json'
    $old = $null
    if (Test-Path -LiteralPath $cfgFile) { $old = Get-Content -LiteralPath $cfgFile -Raw | ConvertFrom-Json }
    if (-not $Zentrale -and $old -and $old.zentrale) { $Zentrale = [string]$old.zentrale }
    if (-not $Zentrale) { $Zentrale = Read-Host 'Adresse der Zentrale (https://..., leer = nur master von GitHub)' }
    $Zentrale = $Zentrale.Trim().TrimEnd('/')
    if ($Zentrale -and $Zentrale -notmatch '^https://[A-Za-z0-9.-]+(:\d+)?$') { throw "Zentrale-Adresse '$Zentrale' ist keine https-Adresse." }
    if (-not $DevDir) { $DevDir = if ($old -and $old.devDir) { [string]$old.devDir } else { Join-Path $env:USERPROFILE 'code\simplemods-dev' } }
    Write-Note "Zentrale: $(if ($Zentrale) { $Zentrale } else { '(keine)' })"
    Write-Note "Dev-Ordner: $DevDir"

    Write-Step 'Werkzeuge pruefen (winget)'
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw 'winget fehlt. Bitte "App-Installer" aus dem Microsoft Store installieren/aktualisieren und den Installer erneut starten.'
    }
    Update-Path
    $git = Find-Exe @('git') @("$env:ProgramFiles\Git\cmd\git.exe", "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe")
    if (-not $git) { Install-WingetPackage 'Git.Git' 'Git' -UserScope; $git = Find-Exe @('git') @("$env:ProgramFiles\Git\cmd\git.exe", "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe") }
    if (-not $git) { throw 'Git konnte nicht installiert werden.' }
    Write-Note "Git: $git"

    $java = Find-JavaHome
    if (-not $java) { Install-WingetPackage 'EclipseAdoptium.Temurin.25.JDK' 'Eclipse Temurin JDK 25' -UserScope; $java = Find-JavaHome }
    if (-not $java) { throw 'JDK 25 (Eclipse Temurin) konnte nicht installiert werden.' }
    Write-Note "JDK 25: $java"

    # the texture build (generateVanillaDerived) and the mod selection need Python 3 with Pillow + numpy
    $python = Find-Python
    if (-not $python) {
        $any = Find-Python -Any
        if (-not $any) { Install-WingetPackage 'Python.Python.3.12' 'Python 3.12' -UserScope; $any = Find-Python -Any }
        if ($any) {
            Write-Note 'installiere Pillow + numpy (pip, nur fuer diesen Benutzer) ...'
            $o = Invoke-Quiet $any @('-m', 'pip', 'install', '--user', '--quiet', '--disable-pip-version-check', 'pillow', 'numpy')
            if ($script:LastExit -ne 0) { Write-Note ($o -join "`n") }
            $python = Find-Python
        }
    }
    if ($python) { Write-Note "Python: $python" } else { Write-Note 'WARNUNG: kein Python mit Pillow/numpy - der Build nimmt Platzhalter-Texturen, die Mod-Auswahl bleibt aus.' }

    $cf = ''
    if ($Zentrale) {
        $cf = Find-Exe @('cloudflared') @("${env:ProgramFiles(x86)}\cloudflared\cloudflared.exe", "$env:ProgramFiles\cloudflared\cloudflared.exe")
        if (-not $cf) { Install-WingetPackage 'Cloudflare.cloudflared' 'cloudflared (Anmeldung an der Zentrale)'; $cf = Find-Exe @('cloudflared') @("${env:ProgramFiles(x86)}\cloudflared\cloudflared.exe", "$env:ProgramFiles\cloudflared\cloudflared.exe") }
        if (-not $cf) { throw 'cloudflared konnte nicht installiert werden.' }
        Write-Note "cloudflared: $cf"
    }

    Write-Step 'Einstellungen und Starter'
    $cfg = [ordered]@{ zentrale = $Zentrale; devDir = $DevDir; javaHome = $java; python = $python; cloudflared = $cf; installed = (Get-Date).ToString('s') }
    $cfg | ConvertTo-Json | Set-Content -LiteralPath $cfgFile -Encoding UTF8
    $starter = Join-Path $Home2 'starter.ps1'
    $local = Join-Path $PSScriptRoot 'starter.ps1'
    if ($PSScriptRoot -and (Test-Path -LiteralPath $local)) {
        Copy-Item -LiteralPath $local -Destination $starter -Force
        Write-Note "Starter aus $local"
    } elseif ($Zentrale) {
        $tmp = "$starter.new"
        Get-ZentraleFile $Zentrale $cf 'setup/starter.ps1' $tmp
        Move-Item -LiteralPath $tmp -Destination $starter -Force
        Write-Note 'Starter aus der Zentrale geladen'
    } else {
        throw 'starter.ps1 nicht gefunden: install.ps1 aus dem Repo-Ordner tools\zentrale-web\starter starten oder eine Zentrale angeben.'
    }
    Register-Protocol $starter
    Write-Note 'Protokoll simplemods:// fuer diesen Benutzer registriert'

    Write-Step 'Code holen (einmalig etwas groesser)'
    $ref = if ($Zentrale) { 'wave' } else { 'master' }
    $ps = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    & $ps -NoProfile -ExecutionPolicy Bypass -File $starter "simplemods://fetch?ref=$ref"
    if ($LASTEXITCODE -ne 0) { throw "Code holen fehlgeschlagen (Details: $Home2\starter.log)" }

    Write-Step 'Fertig'
    Write-Note 'Die Knoepfe "Laptop-Start" der Zentrale funktionieren jetzt auf diesem PC.'
    Write-Note 'Der erste Start dauert laenger (Gradle laedt Minecraft und Abhaengigkeiten).'
    Show-Popup "SimpleMods ist eingerichtet.`n`nDie Knoepfe 'Laptop-Start' in der Zentrale funktionieren jetzt.`nDev-Ordner: $DevDir" 64
}

# dot-sourced (tests): only define the functions
if ($MyInvocation.InvocationName -eq '.') { return }

try { Main $Zentrale $DevDir }
catch {
    $msg = $_.Exception.Message
    try { Add-Content -LiteralPath $LogFile -Value "FEHLER: $msg" -Encoding UTF8 } catch { }
    Write-Host "FEHLER: $msg" -ForegroundColor Red
    Show-Popup "$msg`n`nLog: $LogFile"
    exit 1
}
