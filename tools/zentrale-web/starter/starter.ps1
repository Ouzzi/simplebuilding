# SimpleMods starter: handles the simplemods:// links of the Zentrale (Windows, PowerShell 5.1 or 7).
#   simplemods://start?ref=wave|master[&off=<mod ids>]   fetch the state, apply the mod selection, start the dev client
#   simplemods://fetch?ref=wave|master                   only fetch
# Set up by install.ps1 (same folder in the repo): settings in ~\.simplemods\config.json, log ~\.simplemods\starter.log.
# master comes from the public GitHub repo; the working state (wave) is a git bundle from the Zentrale, fetched with
# the Cloudflare Access token of "cloudflared access login" (no secret is stored by this script).
# Errors show up as a Windows message box. Keep this file ASCII (PowerShell 5.1 reads it as ANSI).
param([string]$Link = '')

$ErrorActionPreference = 'Stop'
$Home2 = Join-Path $env:USERPROFILE '.simplemods'
$LogFile = Join-Path $Home2 'starter.log'
$ConfigFile = Join-Path $Home2 'config.json'
$GitHubUrl = 'https://github.com/Ouzzi/simplebuilding.git'
$WaveRef = 'refs/remotes/zentrale/claude-wave1'
$MasterRef = 'refs/remotes/github/master'

function Write-StarterLog([string]$Text) {
    $line = '{0:yyyy-MM-dd HH:mm:ss} {1}' -f (Get-Date), $Text
    Write-Host $line
    try { Add-Content -LiteralPath $LogFile -Value $line -Encoding UTF8 } catch { }
}

function Show-Popup([string]$Text, [int]$Icon = 16) {
    # 16 = error, 64 = info; WScript.Shell needs no extra assembly
    try { [void](New-Object -ComObject WScript.Shell).Popup($Text, 0, 'SimpleMods-Starter', $Icon) } catch { Write-Host $Text }
}

# A message box that does not wait (the Gradle output keeps flowing while it is open).
function Show-PopupAsync([string]$Text, [int]$Icon = 48) {
    $ps = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    $code = "[void](New-Object -ComObject WScript.Shell).Popup('{0}', 0, 'SimpleMods-Starter', {1})" -f ($Text -replace "'", "''"), $Icon
    $enc = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($code))
    try { Start-Process -FilePath $ps -ArgumentList @('-NoProfile', '-WindowStyle', 'Hidden', '-EncodedCommand', $enc) -WindowStyle Hidden } catch { Write-StarterLog $Text }
}

# Runs a native program; output goes to the log (and the console). Throws on a non-zero exit code.
# OnLine (optional) sees every output line, e.g. to react to a warning while the program still runs.
function Invoke-Native([string]$Exe, [string[]]$Arguments, [switch]$Quiet, [switch]$AllowFail, [scriptblock]$OnLine) {
    Write-StarterLog "> $Exe $($Arguments -join ' ')"
    $old = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    $lines = New-Object System.Collections.Generic.List[string]
    try {
        & $Exe @Arguments 2>&1 | ForEach-Object {
            $l = "$_"; $lines.Add($l)
            if (-not $Quiet) { Write-StarterLog "  $l" }
            if ($OnLine) { & $OnLine $l }
        }
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    if ($code -ne 0 -and -not $AllowFail) { throw "'$Exe $($Arguments -join ' ')' ist fehlgeschlagen (Exit $code). Details: $LogFile" }
    $script:LastExit = $code
    return ,$lines
}

function Read-Config {
    $cfg = @{ zentrale = ''; devDir = (Join-Path $env:USERPROFILE 'code\simplemods-dev'); javaHome = ''; python = ''; cloudflared = '' }
    if (Test-Path -LiteralPath $ConfigFile) {
        $j = Get-Content -LiteralPath $ConfigFile -Raw | ConvertFrom-Json
        foreach ($p in $j.PSObject.Properties) { $cfg[$p.Name] = [string]$p.Value }
    }
    return $cfg
}

function Update-Path {
    # winget installs change PATH only for new processes
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')
}

function Find-Exe([string]$Configured, [string[]]$Names, [string[]]$Candidates) {
    if ($Configured -and (Test-Path -LiteralPath $Configured)) { return $Configured }
    foreach ($n in $Names) { $c = Get-Command $n -ErrorAction SilentlyContinue | Select-Object -First 1; if ($c) { return $c.Source } }
    foreach ($c in $Candidates) { if ($c -and (Test-Path -LiteralPath $c)) { return $c } }
    return ''
}

function Find-JavaHome([string]$Configured) {
    if ($Configured -and (Test-Path -LiteralPath (Join-Path $Configured 'bin\java.exe'))) { return $Configured }
    $roots = @("$env:ProgramFiles\Eclipse Adoptium", "$env:LOCALAPPDATA\Programs\Eclipse Adoptium", "$env:ProgramFiles\Java")
    foreach ($r in $roots) {
        if (-not (Test-Path -LiteralPath $r)) { continue }
        $d = Get-ChildItem -LiteralPath $r -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($d -and (Test-Path -LiteralPath (Join-Path $d.FullName 'bin\java.exe'))) { return $d.FullName }
    }
    return ''
}

# Python with Pillow + numpy for the texture build (generateVanillaDerived): the configured one, py -3, python/python3
# on PATH (not the Microsoft Store alias) or a per-user install. When a Python lacks the modules they are installed
# once for this user (pip --user). Returns the python.exe path, or '' (the build then ships grey placeholder textures).
function Test-PythonModules([string]$Exe) {
    if (-not $Exe -or -not (Test-Path -LiteralPath $Exe)) { return $false }
    [void](Invoke-Native $Exe @('-c', 'import PIL, numpy') -Quiet -AllowFail)
    return ($script:LastExit -eq 0)
}
function Resolve-Python([string]$Configured) {
    $cands = @()
    if ($Configured) { $cands += $Configured }
    $py = Find-Exe '' @('py') @("$env:LOCALAPPDATA\Programs\Python\Launcher\py.exe", "$env:SystemRoot\py.exe")
    if ($py) {
        $o = Invoke-Native $py @('-3', '-c', 'import sys; print(sys.executable)') -Quiet -AllowFail
        if ($script:LastExit -eq 0) { $cands += ($o -join '').Trim() }
    }
    foreach ($n in @('python', 'python3')) {
        foreach ($c in @(Get-Command $n -All -ErrorAction SilentlyContinue)) {
            if ($c.Source -notmatch 'WindowsApps') { $cands += $c.Source }
        }
    }
    $cands += @(Get-ChildItem "$env:LOCALAPPDATA\Programs\Python\Python3*\python.exe" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    $cands = @($cands | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -Unique)
    foreach ($c in $cands) { if (Test-PythonModules $c) { return $c } }
    foreach ($c in $cands) {
        Write-StarterLog "Pillow/numpy fehlen in $c - installiere sie fuer diesen Benutzer (pip --user) ..."
        [void](Invoke-Native $c @('-m', 'pip', 'install', '--user', '--quiet', '--disable-pip-version-check', 'pillow', 'numpy') -AllowFail)
        if (Test-PythonModules $c) { return $c }
    }
    return ''
}

function Save-ConfigValue([string]$Name, [string]$Value) {
    try {
        $j = if (Test-Path -LiteralPath $ConfigFile) { Get-Content -LiteralPath $ConfigFile -Raw | ConvertFrom-Json } else { New-Object psobject }
        $j | Add-Member -NotePropertyName $Name -NotePropertyValue $Value -Force
        $j | ConvertTo-Json | Set-Content -LiteralPath $ConfigFile -Encoding UTF8
    } catch { Write-StarterLog "config.json nicht aktualisiert: $($_.Exception.Message)" }
}

function Find-Cloudflared([string]$Configured) {
    return Find-Exe $Configured @('cloudflared') @("${env:ProgramFiles(x86)}\cloudflared\cloudflared.exe", "$env:ProgramFiles\cloudflared\cloudflared.exe", "$env:LOCALAPPDATA\Microsoft\WinGet\Links\cloudflared.exe")
}

# Access token for the Zentrale (Cloudflare Access): cached by cloudflared, browser login when missing or expired.
function Get-AccessToken([string]$Zentrale, [string]$Cloudflared, [switch]$Fresh) {
    if (-not $Fresh) {
        $t = Invoke-Native $Cloudflared @('access', 'token', "-app=$Zentrale") -Quiet -AllowFail
        $tok = ($t -join '').Trim()
        if ($script:LastExit -eq 0 -and $tok -match '^ey[\w-]+\.[\w-]+\.[\w-]+$') { return $tok }
    }
    Write-StarterLog 'Anmeldung an der Zentrale im Browser (Cloudflare Access) ...'
    [void](Invoke-Native $Cloudflared @('access', 'login', $Zentrale) -Quiet -AllowFail)
    $t = Invoke-Native $Cloudflared @('access', 'token', "-app=$Zentrale") -Quiet -AllowFail
    $tok = ($t -join '').Trim()
    if ($script:LastExit -ne 0 -or $tok -notmatch '^ey[\w-]+\.[\w-]+\.[\w-]+$') { throw 'Keine Anmeldung an der Zentrale (cloudflared access login). Bitte im Browser anmelden und erneut versuchen.' }
    return $tok
}

# Downloads <Zentrale>/<Path> with the Access token; one re-login when Access answers with its login page.
function Get-FromZentrale([hashtable]$Cfg, [string]$Path, [string]$OutFile) {
    $cf = Find-Cloudflared $Cfg.cloudflared
    if (-not $cf) { throw 'cloudflared fehlt (fuer den Arbeitsstand aus der Zentrale). Installer erneut ausfuehren.' }
    $url = $Cfg.zentrale.TrimEnd('/') + '/' + $Path
    for ($try = 0; $try -lt 2; $try++) {
        $tok = Get-AccessToken $Cfg.zentrale $cf -Fresh:($try -gt 0)
        try {
            $r = Invoke-WebRequest -Uri $url -Headers @{ 'cf-access-token' = $tok } -OutFile $OutFile -PassThru -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 600
            $type = [string]$r.Headers['Content-Type']
            if ([int]$r.StatusCode -eq 200 -and $type -notmatch 'text/html') { return }
            Write-StarterLog "Zentrale antwortet nicht mit der Datei ($url, $([int]$r.StatusCode) $type), neue Anmeldung"
        } catch { Write-StarterLog "Download $url fehlgeschlagen: $($_.Exception.Message)" }
    }
    throw "Konnte $url nicht laden (Zugang zur Zentrale?). Details: $LogFile"
}

# The mod selection of the Zentrale (simplemods://start?...&off=a,b): modselect.py writes integration/enabled-mods.json
# and the "Enable Simple X" switches. Errors are logged; the start continues with the previous selection.
function Invoke-ModSelection([string]$Url, [string]$Repo, [string]$Python) {
    $off = ''
    if ($Url -match '[?&]off=([^&]*)') { $off = [Uri]::UnescapeDataString($Matches[1]) }
    if ($off -notmatch '^[a-z0-9_,]*$') { Write-StarterLog '[modselect] ungueltige off-Liste ignoriert'; $off = '' }
    $msPy = Join-Path $Repo 'tools\zentrale-web\modselect.py'
    if (-not (Test-Path -LiteralPath $msPy)) { Write-StarterLog '[modselect] in diesem Stand noch nicht vorhanden, alle Mods an'; return }
    if (-not $Python) { Write-StarterLog '[modselect] kein Python gefunden, alle Mods an'; return }
    # "--off=" as one token: PowerShell 5.1 drops empty arguments
    [void](Invoke-Native $Python @($msPy, 'apply', '--repo', $Repo, "--off=$off") -AllowFail)
    if ($script:LastExit -ne 0) { Write-StarterLog "[modselect] fehlgeschlagen (Exit $($script:LastExit)), vorherige Auswahl bleibt" }
}

function Sync-Repo([hashtable]$Cfg, [string]$Ref, [string]$Git) {
    $dev = $Cfg.devDir
    if (-not (Test-Path -LiteralPath (Join-Path $dev '.git'))) {
        Write-StarterLog "Klone $GitHubUrl nach $dev (einmalig, dauert etwas) ..."
        New-Item -ItemType Directory -Force -Path (Split-Path $dev) | Out-Null
        [void](Invoke-Native $Git @('clone', '--branch', 'master', $GitHubUrl, $dev))
    }
    Set-Location -LiteralPath $dev
    # the starter owns this folder: only the mod selection file may differ from the commit
    [void](Invoke-Native $Git @('checkout', '--', 'integration/enabled-mods.json') -Quiet -AllowFail)
    $dirty = Invoke-Native $Git @('status', '--porcelain', '--untracked-files=no') -Quiet
    if ($dirty.Count -gt 0) {
        throw "Der Dev-Ordner $dev hat lokale Aenderungen ($($dirty[0]) ...). Bitte sichern oder verwerfen (git stash), dann erneut starten."
    }
    [void](Invoke-Native $Git @('fetch', '--quiet', $GitHubUrl, "+master:$MasterRef"))
    $target = $MasterRef
    if ($Ref -eq 'wave') {
        if (-not $Cfg.zentrale) { throw 'Keine Zentrale eingetragen (config.json). Installer erneut ausfuehren.' }
        $tmp = Join-Path $Home2 'wave.json'
        Get-FromZentrale $Cfg 'setup/wave.json' $tmp
        $info = Get-Content -LiteralPath $tmp -Raw | ConvertFrom-Json
        if ($info.commit -notmatch '^[0-9a-f]{40}$') { throw 'wave.json der Zentrale ist ungueltig.' }
        $have = (Invoke-Native $Git @('rev-parse', '--verify', '--quiet', "$WaveRef^{commit}") -Quiet -AllowFail) -join ''
        if ($have.Trim() -ne $info.commit) {
            $bundle = Join-Path $Home2 'wave.bundle'
            Write-StarterLog ("Lade Arbeitsstand {0} ({1:N1} MB) ..." -f $info.commit.Substring(0, 9), ($info.bytes / 1MB))
            Get-FromZentrale $Cfg 'setup/wave.bundle' $bundle
            [void](Invoke-Native $Git @('fetch', '--quiet', $bundle, "+refs/heads/$($info.branch):$WaveRef"))
            Remove-Item -LiteralPath $bundle -Force -ErrorAction SilentlyContinue
        } else { Write-StarterLog 'Arbeitsstand ist schon aktuell.' }
        $target = $WaveRef
    }
    [void](Invoke-Native $Git @('checkout', '--quiet', '--force', '-B', 'simplemods-run', $target))
    $head = (Invoke-Native $Git @('log', '-1', '--format=%h %s') -Quiet) -join ''
    Write-StarterLog "Stand: $head"
    # keep the installed starter in step with the fetched state (takes effect from the next link)
    $fresh = Join-Path $dev 'tools\zentrale-web\starter\starter.ps1'
    if ((Test-Path -LiteralPath $fresh) -and $PSCommandPath -and ((Get-FileHash $fresh).Hash -ne (Get-FileHash $PSCommandPath).Hash)) {
        Copy-Item -LiteralPath $fresh -Destination $PSCommandPath -Force
        Write-StarterLog 'Starter aus dem Stand aktualisiert.'
    }
}

function Main([string]$Url) {
    New-Item -ItemType Directory -Force -Path $Home2 | Out-Null
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    Write-StarterLog "==== $Url"
    if ($Url -notmatch '^simplemods://(start|fetch)/?(\?.*)?$') { throw "Unbekannter Link: '$Url' (erwartet simplemods://start?ref=wave)" }
    $action = $Matches[1]
    $ref = 'wave'
    if ($Url -match '[?&]ref=([a-z]+)') { $ref = $Matches[1] }
    if ($ref -notin @('wave', 'master')) { throw "Unbekannter Stand ref=$ref (wave oder master)" }

    $mutex = New-Object System.Threading.Mutex($false, 'Local\SimpleModsStarter')
    if (-not $mutex.WaitOne(0)) { Show-Popup 'Der Starter laeuft schon (anderes Fenster).' 64; return }
    try {
        $cfg = Read-Config
        Update-Path
        $git = Find-Exe '' @('git') @("$env:ProgramFiles\Git\cmd\git.exe", "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe")
        if (-not $git) { throw 'Git fehlt. Installer erneut ausfuehren.' }
        Sync-Repo $cfg $ref $git
        if ($action -eq 'fetch') { Write-StarterLog 'Fertig (nur geholt).'; return }

        $java = Find-JavaHome $cfg.javaHome
        if (-not $java) { throw 'JDK 25 (Eclipse Temurin) fehlt. Installer erneut ausfuehren.' }
        $env:JAVA_HOME = $java
        $env:Path = (Join-Path $java 'bin') + ';' + $env:Path
        # only a verified Python goes to the build: an unchecked one (e.g. the Store alias) cost the real textures (19.10.)
        $python = Resolve-Python $cfg.python
        if ($python) {
            $env:SB_PYTHON = $python
            if ($python -ne $cfg.python) { Save-ConfigValue 'python' $python }
            Write-StarterLog "Python fuer die Texturen: $python"
        } else {
            Remove-Item Env:SB_PYTHON -ErrorAction SilentlyContinue
            Write-StarterLog 'WARNUNG: kein Python mit Pillow/numpy - der Build zeichnet graue Platzhalter-Texturen.'
            Show-PopupAsync "Kein Python mit Pillow und numpy gefunden.`n`nDas Spiel startet, aber viele Bloecke/Items zeigen graue Schachmuster.`nAbhilfe: Installer erneut ausfuehren (installiert Python 3.12 + Pillow + numpy)."
        }
        Invoke-ModSelection $Url $cfg.devDir $python
        Write-StarterLog 'Starte den Dev-Client (:integration:runIntegrationClient) ...'
        $script:PlaceholderNoticeShown = $false
        $watch = {
            param($line)
            if (-not $script:PlaceholderNoticeShown -and $line -match 'GREY PLACEHOLDER TEXTURES') {
                $script:PlaceholderNoticeShown = $true
                Show-PopupAsync "Der Build hat graue Platzhalter-Texturen erzeugt (aus Vanilla abgeleitete Texturen fehlen).`n`nGrund und Abhilfe stehen im Log:`n$LogFile`n(Zeilen 'GREY PLACEHOLDER TEXTURES')."
            }
        }
        [void](Invoke-Native (Join-Path $cfg.devDir 'gradlew.bat') @(':integration:runIntegrationClient', '--console=plain') -OnLine $watch)
        Write-StarterLog 'Dev-Client beendet.'
    } finally { $mutex.ReleaseMutex(); $mutex.Dispose() }
}

# dot-sourced (tests): only define the functions
if ($MyInvocation.InvocationName -eq '.') { return }

try { Main $Link }
catch {
    $msg = $_.Exception.Message
    Write-StarterLog "FEHLER: $msg"
    Show-Popup "$msg`n`nLog: $LogFile"
    exit 1
}
