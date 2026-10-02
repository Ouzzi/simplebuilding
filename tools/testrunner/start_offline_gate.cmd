@echo off
rem Startet den unbeaufsichtigten Offline-Testlauf als eigenen, minimierten Prozess (laeuft weiter ohne Hub/Claude/Netz).
rem Aufruf: start_offline_gate.cmd [SHA,SHA ...]   ohne Argument: .ai-runs\offline-queue.txt des Haupt-Checkouts
rem Weitere Schalter direkt am Skript: pwsh -File offline_gate.ps1 -Refs a,b -Profile 263 oder all [-Online]
if "%~1"=="" (
  start "offline-gate" /min pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0offline_gate.ps1"
) else (
  start "offline-gate" /min pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0offline_gate.ps1" -Refs %*
)
echo gestartet. Fortschritt: .ai-runs\offline-status.txt  Ergebnis: .ai-runs\offline-results.md (Haupt-Checkout)
