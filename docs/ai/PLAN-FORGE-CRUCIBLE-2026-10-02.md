# Forge 26.3: Crucible per Vorschlaghammer

## Auftrag und Plan (2026-10-05)

Der erste Bauschlag wird nur auf Forge 26.3 nicht erkannt. Die Ursache im echten
Spielpfad beheben, ohne den vorhandenen GameTest abzuschwaechen. Nur aktueller
Branch `gpt-forgecrucible`, kein Push und keine Clients.

1. CrucibleCompat, SimpleLib-Baubedingungen, Forge-Buendelung und Initialisierung
   verfolgen und alle Crucible-Tests auf Forge reproduzieren.
2. Die belegte Ursache an der engsten gemeinsamen Stelle korrigieren. Betroffene
   Dateien ergeben sich aus dem Befund; 26.2 bleibt ohne Feature-Port.
3. Regression im vorhandenen GameTest sichern, Forge-Crucible-Filter pruefen,
   danach komplette Fabric-/NeoForge-/Forge-26.3-Serverlaeufe.
4. `check -q -PskipWiki` sowie 26.2 Fabric-/NeoForge- und Forge-26.3-Compile.
   Ergebnisse lesen, dokumentieren und auf dem aktuellen Branch committen.

Risiken: Forge-Registry-/Tag-Lebenszyklus, doppelte Bibliotheksinstanzen,
abweichende Dev-/Jar-Classpaths. Keine Aenderung an Rezeptkosten oder Testfristen.
Annahme: Server-GameTests decken den autorisierten Testzentralen-Neubau ab;
Besitzerwelt und visuelle Abnahme bleiben ohne Client unberuehrt.

## Befund und Umsetzung

Der unveraenderte Forge-Filter reproduziert 13/14 bestandene Tests und
`strike 1 not taken on tick 0` (Lauf `2026-10-05T05-06-16Z-adb6`).
Im Serverlog stehen `Missing metadata in pack mod:simplelib` und
`Missing data pack mod:simplelib`. Jar-in-Jar laedt SimpleLib erfolgreich;
auch Enderite-Tiers, deaktivierte Axtwege und Cauldron-Partner funktionieren.
Es fehlen ausschliesslich die Pack-Metadaten: Forge verwirft damit SimpleLibs
Ressourcen einschliesslich `simplelib:crucible_walls`. Der erste Bauschlag
scheitert an `CrucibleBlankBlock.fitsNext`, nicht am Vorschlaghammer-Tag.

Fix: `modules/simplelib/forge/src/main/resources/pack.mcmeta` nach dem
vorhandenen Forge-Modulmuster. Wirkt sowohl im Dev-Lauf als auch im gebuendelten
Spieler-Jar, ohne Sonderfall im Gameplay oder Aenderung an der 26.2-Bruecke.
Der bestehende Bautest prueft jetzt explizit beide Material-Tags, ruft
`SledgehammerItem.useOn` auf und kontrolliert den Haltbarkeitsverbrauch nach
jedem der sechs Schlaege. Ergebnisblock und Gesamtverbrauch bleiben geprueft.

Umgebung: `rtk` und `rg` sind im Sandbox-PATH nicht verfuegbar; native Git- und
gezielte PowerShell-Suchen genutzt. Der erste `python`-Aufruf landete im
Windows-Store-Stub und wurde beendet; Tests nutzen den installierten
`C:/Users/o_o/.local/bin/python3.12.exe`-Launcher mit Java 25 / Forge Java 8.

## Verifikation

- Forge-Crucible nach Fix: Lauf `2026-10-05T05-08-57Z-7073`,
  `alles gruen: 14/14 bestanden, 0 rot`. Keine fehlenden SimpleLib-Pack-Metadaten
  mehr im Serverlog. Das gebaute SimpleLib-Forge-Jar enthaelt `pack.mcmeta`
  und beide Material-Tags; der Dev-Lauf verwendet genau dieses verschachtelte Jar.
- Vollstaendige Server: Lauf `2026-10-05T05-11-37Z-178d`, Fabric 960/960,
  NeoForge 960/960, Forge 961/961: `alles gruen: 2881/2881 bestanden, 0 rot`.
  Frische Reports bestaetigen auf allen drei Loadern den Hammer-Bautest,
  `test_centre_game_test_the_whole_centre_builds_and_matches_its_plan` und
  `test_centre_game_test_every_mod_item_and_block_has_its_place_in_the_test_centre`.
- Wiki `--all` und `--all --check`: Exit 0,
  `wiki: up to date, everything documented.` Nur Zeilenenden normalisiert,
  kein inhaltlicher Wiki-Diff.
- Gate: `gradlew.bat check -q -PskipWiki :compileJava :neoforge:compileJava
  -Pforge263=true :mc26_3:forge:compileJava
  -PwikiPython=C:/Users/o_o/code/simplebuilding/.ai-runs/venv/Scripts/python.exe`:
  `GRADLE_EXIT=0`. Rohlog `.ai-runs/forgecrucible-gate.log`.
  Bestehende Veraltungs-/EnvType-Warnungen, keine fehlgeschlagenen Checks.
- Kein Client, kein Port, kein Push/Merge. Keine Texturen veraendert, daher
  keine Vorschau. Testzentralen-Neubau nur in Testwelten, nicht in der Besitzerwelt.
