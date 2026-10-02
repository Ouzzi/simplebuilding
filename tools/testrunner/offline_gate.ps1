#Requires -Version 7
# Unbeaufsichtigter Testlauf (auch ohne Netz) fuer eine oder mehrere SHAs.
#
#   pwsh -File tools\testrunner\offline_gate.ps1 -Refs 42358f50,abc1234 [-Profile 263|all] [-Online]
#   ohne -Refs: liest .ai-runs\offline-queue.txt (eine SHA pro Zeile, # = Kommentar)
#   Starter: tools\testrunner\start_offline_gate.cmd, oder die Launch- und Testzentrale (Bereich "Offline-Testlauf").
#
# Laeuft im eigenen Worktree %TEMP%\sbgate-offline (stoert weder Haupt-Checkout noch %TEMP%\sbgate).
# Gradle mit --offline (warmer Cache noetig: einmal online mit -Online laufen lassen). Forge-Ziele brauchen immer Netz
# (ForgeGradle-Mavenizer) und werden offline uebersprungen.
# Ergebnis:  .ai-runs\offline-results.md   (fuer Menschen: eine Zeile je SHA und Gruppe, plus VERDICT)
#            .ai-runs\offline-results.json (fuer den Hub: Laeufe, je SHA die Gruppen mit Zahlen und Logs)
# Fortschritt: .ai-runs\offline-status.txt   Logs: .ai-runs\offline-logs\   Sperre: .ai-runs\offline-gate.lock (PID)
# .ai-runs ist der Ordner im Haupt-Checkout (auch wenn das Skript aus einem Worktree startet).
# Pfade per Umgebung ueberschreibbar: SB_OFFLINE_RUNS_DIR, SB_OFFLINE_WORKTREE, SB_OFFLINE_JAVA_HOME,
# SB_OFFLINE_JAVA8_HOME, SB_OFFLINE_PYTHON.
# Pushen tut das Skript nie.
param([string[]]$Refs, [ValidateSet('263','all')][string]$Profile = 'all', [switch]$Online)

$ErrorActionPreference = 'Continue'
function EnvOr($name, $default) { $v = [Environment]::GetEnvironmentVariable($name); if ($v) { $v } else { $default } }
$env:JAVA_HOME = EnvOr 'SB_OFFLINE_JAVA_HOME' 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
$env:SIMPLEBUILDING_JAVA8_HOME = EnvOr 'SB_OFFLINE_JAVA8_HOME' 'C:/Users/o_o/.jdks/jdk8u504-b01'
$py = EnvOr 'SB_OFFLINE_PYTHON' 'C:\Users\o_o\AppData\Roaming\uv\python\cpython-3.12-windows-x86_64-none\python.exe'
$env:Path = "$(Split-Path $py);$env:JAVA_HOME\bin;$env:Path"
$env:SIMPLEBUILDING_GRADLE_OFFLINE = if ($Online) { '0' } else { '1' }
# @(...) noetig: ein einzelnes Element kaeme sonst als String zurueck und wuerde zeichenweise gesplattet.
$offlineArg = @(if (-not $Online) { '--offline'; '--configure-on-demand' })

$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$mainRepo = $repo
$common = git -C $repo rev-parse --path-format=absolute --git-common-dir 2>$null
if ($LASTEXITCODE -eq 0 -and $common -and (Split-Path -Leaf $common) -eq '.git') { $mainRepo = Split-Path -Parent $common }
$runs = EnvOr 'SB_OFFLINE_RUNS_DIR' (Join-Path $mainRepo '.ai-runs')
$wt = EnvOr 'SB_OFFLINE_WORKTREE' (Join-Path $env:TEMP 'sbgate-offline')
$logs = Join-Path $runs 'offline-logs'
$results = Join-Path $runs 'offline-results.md'
$resultsJson = Join-Path $runs 'offline-results.json'
$status = Join-Path $runs 'offline-status.txt'
$lock = Join-Path $runs 'offline-gate.lock'
New-Item -ItemType Directory -Force $logs | Out-Null

function Stamp { (Get-Date).ToString('yyyy-MM-dd HH:mm') }
function Iso { (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ') }
function Status($text) { "$(Stamp)  $text" | Set-Content -Encoding utf8 $status }
function Result($line) { "- $(Stamp)  $line" | Add-Content -Encoding utf8 $results }

# --- maschinenlesbares Ergebnis (der Hub liest es; die .md bleibt fuer Menschen) -----------------
$run = [ordered]@{ id = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmss') + "-$PID"; started = (Iso); finished = $null
    mode = $(if ($Online) { 'online' } else { 'offline' }); profile = $Profile; refs = @(); pid = $PID; status = 'running'; shas = @() }
function SaveJson {
    $old = @()
    if (Test-Path $resultsJson) {
        try { $old = @((Get-Content $resultsJson -Raw | ConvertFrom-Json -AsHashtable).runs | Where-Object { $_ -and $_.id -ne $run.id }) }
        catch { $old = @() }
    }
    $all = @($old + @($run)) | Select-Object -Last 30
    $doc = [ordered]@{ schemaVersion = 1; updated = (Iso); runs = @($all) }
    $tmp = "$resultsJson.tmp"
    [IO.File]::WriteAllText($tmp, ($doc | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    Move-Item -Force $tmp $resultsJson
}

# --- Sperre: nur ein Lauf; ein Lock-PID zaehlt nur, wenn der Prozess wirklich dieses Skript ist ----
if (Test-Path $lock) {
    $other = (Get-Content $lock -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ($other -match '^\d+$') {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$other" -ErrorAction SilentlyContinue
        if ($proc -and $proc.CommandLine -match 'offline[_-]gate\.ps1') { Write-Host "laeuft schon (PID $other)"; exit 1 }
    }
}
$PID | Set-Content $lock
$started = $false

try {
    if (-not $Refs) {
        $queue = Join-Path $runs 'offline-queue.txt'
        if (Test-Path $queue) { $Refs = Get-Content $queue | ForEach-Object { ($_ -replace '#.*', '').Trim() } | Where-Object { $_ } }
    }
    $Refs = @($Refs | ForEach-Object { $_ -split '[,\s]+' } | Where-Object { $_ })
    if (-not $Refs) { Write-Host 'keine SHAs (-Refs oder offline-queue.txt)'; exit 1 }
    $run.refs = $Refs
    $started = $true

    $mode = $run.mode
    Result "**Lauf gestartet** ($mode, Profil $Profile): $($Refs -join ', ')"
    SaveJson

    $firstValid = $Refs | Where-Object { git -C $repo rev-parse --verify --quiet "$_^{commit}" *> $null; $LASTEXITCODE -eq 0 } | Select-Object -First 1
    if ($firstValid -and -not (Test-Path $wt)) { git -C $repo worktree add -q --detach $wt $firstValid }

    foreach ($ref in $Refs) {
        $entry = [ordered]@{ ref = $ref; sha = $ref; error = $null; notes = @(); groups = @(); verdict = $null; verdictText = $null }
        $run.shas += $entry
        git -C $repo rev-parse --verify --quiet "$ref^{commit}" *> $null
        $known = $LASTEXITCODE -eq 0
        if ($known) {
            Set-Location $wt
            git checkout -q -- .
            git clean -fdq modules integration 2>$null
            git checkout -q --detach $ref
            $known = $LASTEXITCODE -eq 0
        }
        if (-not $known) {
            Result "$ref  **FEHLER**: SHA nicht gefunden (vorher fetchen/committen)"
            $entry.error = 'SHA nicht gefunden (vorher fetchen/committen)'; $entry.verdict = 'error'; $entry.verdictText = 'FEHLER'
            SaveJson
            continue
        }
        $sha = (git rev-parse --short HEAD).Trim()
        $entry.sha = $sha
        $ok = $true
        if (-not $Online -and -not (Select-String -Quiet -Path tools/testrunner/run.py -Pattern 'SIMPLEBUILDING_GRADLE_OFFLINE')) {
            $note = 'run.py dieser SHA kennt den Offline-Schalter nicht, Testziele laufen mit Netz-Zugriff'
            Result "$sha  Hinweis: $note"
            $entry.notes += $note
        }

        Status "$sha  gradlew check"
        SaveJson
        $log = Join-Path $logs "$sha-check.log"
        if ($Online) { $checkTasks = @('check') }
        else {
            # Offline nie das 26.2-:forge-Projekt konfigurieren: ForgeGradles Mavenizer laedt dabei immer das
            # Launcher-Manifest (kein Offline-Schalter). Also jedes andere Projekt einzeln pruefen.
            $paths = @(':', ':common', ':neoforge', ':mc1_21_11:fabric', ':mc1_21_11:neoforge', ':mc26_3:fabric', ':mc26_3:neoforge', ':framework', ':integration')
            $reg = Get-Content modules/modules.json -Raw | ConvertFrom-Json
            foreach ($m in $reg.modules) {
                if ($m.id -eq 'simplebuilding' -or -not $m.projects) { continue }
                foreach ($pr in $m.projects.PSObject.Properties) { if ($pr.Name -ne 'forge') { $paths += $pr.Value } }
            }
            $checkTasks = @($paths | ForEach-Object { if ($_ -eq ':') { ':check' } else { "$_`:check" } })
        }
        & .\gradlew.bat @offlineArg @checkTasks -q *> $log
        $ck = $LASTEXITCODE -eq 0
        Result "$sha  check: $(if ($ck) { 'OK' } else { "**FEHLER** ($log)" })"
        $entry.groups += [ordered]@{ name = 'check'; kind = 'check'; targets = ($checkTasks -join ','); ok = $ck
            passed = $null; total = $null; failed = $null; red = @(); log = (Split-Path -Leaf $log) }
        $ok = $ok -and $ck
        SaveJson

        # Forge-Ziele brauchen Netz (Mavenizer, s. o.): offline nur vermerkt, nach Rueckkehr online nachholen.
        $forge = if ($Online) { ',forge-263' } else { '' }
        $groups = [ordered]@{ 'kern-263' = "fabric-263,neoforge-263$forge" }
        $listing = & $py tools/testrunner/run.py --list 2>&1 | Out-String
        $mods = [regex]::Matches($listing, '(?m)^\s+(module-[a-z0-9_]+-(?:fabric|neoforge)-263)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
        $groups['module-263'] = (@('integration-263') + $mods) -join ','
        if ($Profile -eq 'all') {
            $groups['linie-262'] = "fabric-262,neoforge-262$(if ($Online) { ',forge-262' })"
            $groups['linie-12111'] = 'fabric-12111,neoforge-12111'
        }
        foreach ($g in $groups.Keys) {
            Status "$sha  Tests $g"
            $log = Join-Path $logs "$sha-$g.log"
            & $py tools/testrunner/run.py --targets $groups[$g] *> $log
            $text = Get-Content $log -Raw
            if ($null -eq $text) { $text = '' }
            $m = [regex]::Match($text, 'alles gruen: (\d+)/(\d+)')
            $n = [regex]::Match($text, 'NICHT gruen: (\d+)/(\d+) bestanden, (\d+) rot')
            $redLines = @($text -split "`r?`n" | Where-Object { $_ -cmatch '^\s+(ROT|FEHLER) ' } | ForEach-Object { $_.Trim() })
            $group = [ordered]@{ name = $g; kind = 'tests'; targets = $groups[$g]; ok = $m.Success; passed = $null; total = $null
                failed = $null; red = @($redLines | Select-Object -First 40); log = (Split-Path -Leaf $log) }
            if ($m.Success) {
                $group.passed = [int]$m.Groups[1].Value; $group.total = [int]$m.Groups[2].Value; $group.failed = 0
                Result "$sha  $g`: gruen $($m.Groups[1].Value)/$($m.Groups[2].Value)"
            }
            else {
                if ($n.Success) { $group.passed = [int]$n.Groups[1].Value; $group.total = [int]$n.Groups[2].Value; $group.failed = [int]$n.Groups[3].Value }
                $summary = if ($n.Success) { @($n.Value) } else { @() }
                $red = (@($redLines | Select-Object -First 6) + $summary) -join ' | '
                if (-not $red) { $red = 'kein Ergebnis im Log' }
                Result "$sha  $g`: **ROT** - $red ($log)"
                # Nur 26.3 entscheidet ueber den Push; 26.2/1.21.11 sind Port-Linien und werden nur berichtet.
                if ($g -like '*-263') { $ok = $false }
            }
            $entry.groups += $group
            SaveJson
        }
        $v = if (-not $ok) { 'RED' } elseif ($Online) { 'GREEN' } else { 'GREEN ohne Forge (forge-263 online nachholen, ~10 min)' }
        $entry.verdict = if (-not $ok) { 'red' } elseif ($Online) { 'green' } else { 'green-no-forge' }
        $entry.verdictText = $v
        Result "$sha  **VERDICT 26.3: $v**"
        SaveJson
    }
    $run.status = 'done'
    Status 'fertig'
}
finally {
    if ($started) {
        if ($run.status -ne 'done') { $run.status = 'aborted' }
        $run.finished = Iso
        try { SaveJson } catch { }
    }
    Remove-Item $lock -ErrorAction SilentlyContinue
}
