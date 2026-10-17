# Plan: Simple Tweaks Teil 1 – Admin/Schutz (17.10.2026, Sitzung A, Branch `claude-tweaks-admin`)

Auftrag: Queue-Punkt „Simple Tweaks Teil 1 Admin/Schutz“ (Besitzer-Auswahl 17.10.): T10, T11, T13–T18, T34.
Nachtrag Besitzer 17.10.: Alles, was nur auf Befehl wirkt, ist standardmäßig an → T34 an (misst nur während /lag).
Parallel-Branch `claude-tweaks-play` (Teil 2) erweitert dasselbe Modul – deshalb alles Neue unter eigenem Paket,
eigener Config-Datei, eigenem Mixin-Config und eigener Test-Liste; Loader-Einstiege bekommen je **eine** Zeile.

## Ist-Zustand (Survey)
- `modules/simpletweaks`: Claims (Server-Autorität, `ClaimConfig` JSON unter `config/`, `ClaimCommands` mit
  `Commands.hasPermission`, Shared-Mixins für Fabric/NeoForge/Forge, Tests als `Map<String,Consumer<GameTestHelper>>`,
  Fabric `@GameTest`-Klassen, NeoForge/Forge registrieren die Map), Deed-/Tisch-UI im simplelib-Kasten (`UiBoxes`, `UiPalette`).
- Menüs mit eigenem Screen: Muster `simpleshops` (`MenuType` in `BuiltInRegistries.MENU`, Fabric direkt, Neo/Forge im
  `RegisterEvent`, Screens per `MenuScreens`/`RegisterMenuScreensEvent`/`FMLClientSetupEvent`).
- Rechte: kein LuckPerms/fabric-permissions-api im Repo → Vanilla-OP-Stufen (`PermissionLevel` 0–4), je Feature konfigurierbar.
- Vorbilder (nur Ideen, kein Code übernommen): Modrinth „InvSee“/„InvRestore“, CoreProtect (Inspect-Modus, Rollback).

## Gemeinsames
- Paket `com.simplebuilding.modules.simpletweaks.admin` (+ Unterpakete je Feature), Mixins `admin.mixin` mit
  eigener `simpletweaks.admin.mixins.json`.
- Config `config/simpletweaks-admin.json`, ein Abschnitt je Feature (`blockLog`, `deathBackups`, `invsee`, `vanish`,
  `freeze`, `restart`, `backups`, `maintenance`, `lagFinder`), jeder mit `enabled` und `permission` (OP-Stufe 0–4).
  Fehlt die Datei, wird sie mit Standardwerten geschrieben; Werte werden geklemmt (Regel 3.8, Obergrenzen).
- Weltdaten unter `<world>/simpletweaks/` (`blocklog/`, `deaths/`, `admin-state.json` für Vanish/Freeze/Wartung).
- `/tweaks …` bekommt **kein** `requires` an der Wurzel (Brigadier führt gleichnamige Wurzeln zusammen; die
  erste Anforderung gewinnt) – Rechte hängen an den Unterbefehlen, damit Teil 2 eigene `/tweaks`-Zweige anhängen kann.
- Lang-Schlüssel `simpletweaks.admin.*` (EN/DE), `check_data.py` lässt dieses Präfix zu.
- Ein Feature, das per Config aus ist: keine Befehle, Mixins tun nichts (ein Boolean-Check), keine Dateien.
- Super-Mod-Schalter `SubMods.enabled("simpletweaks")` aus → alle Admin-Features wirkungslos.

## T10 Block-/Truhen-Protokoll (Standard an)
- Erfasst: Abbau (`ServerPlayerGameMode.destroyBlock`, alter Zustand + Blockentity-NBT bis 32 KB), Setzen
  (`BlockItem.placeBlock`), Container nehmen/legen (Snapshot beim Öffnen eines Menüs, Diff beim Schließen; nur Slots,
  deren Container eine Blockentity ist, Doppeltruhen über Accessor).
- Datensatz (TSV, eine Zeile): Zeit ms, Aktion `B/P/T/D`, UUID, Name, x y z, Block-/Item-ID, Anzahl, Zusatz (Zustand bzw. SNBT).
- Ablage: `<world>/simpletweaks/blocklog/<dim>/<rx>_<rz>/<yyyy-MM-dd>.tsv` (Region 512×512 = Index, Tagesdatei = Zeitindex).
  Schreiben gepuffert (Queue) und alle 5 s auf einem eigenen Thread angehängt; Lesen auf demselben Thread (Reihenfolge).
  Aufbewahrung `retentionDays` (Standard 14, 1–365), alte Tagesdateien werden beim Start und täglich gelöscht.
- `/inspect` (Stufe `permission`, Standard 2) schaltet den Prüfmodus: Linksklick/Rechtsklick auf einen Block zeigt dessen
  Verlauf (7 Zeilen je Seite, `/inspect page <n>`), ohne abzubauen oder zu benutzen.
- `/tweaks rollback <player> <time> [radius]` (Stufe `rollbackPermission`, Standard 3): Zeit `30m`, `2h`, `1d`;
  ohne Radius alle Dimensionen. Rückwärts: gesetzte Blöcke weg (nur wenn noch derselbe Block), abgebaute zurück
  (nur auf Luft/ersetzbar, inkl. Blockentity-Daten), genommene Items zurück in den Container, gelegte heraus.
  Obergrenze `rollbackMaxChanges` (Standard 20000).
- Tests: Abbau/Setzen/Truhe werden protokolliert und gefunden; Inspect zeigt Verlauf und baut nicht ab; Rollback stellt
  Block und Truhe wieder her; Aufbewahrung löscht alte Tage; Feature aus = keine Datei.

## T11 Inventar-Sicherung beim Tod (Standard an)
- `ServerPlayer.die` (HEAD, vor dem Fallenlassen): Slots 0–40 (Inventar, Rüstung, Nebenhand), XP (Stufe, Fortschritt,
  gesamt), Zeit, Ursache (Todesnachricht), Position, Dimension. Je Spieler die letzten `keep` (Standard 5, 1–50) Tode in
  `<world>/simpletweaks/deaths/<uuid>.dat` (NBT, atomar geschrieben). Leere Inventare werden auch gesichert (Ursache zählt).
- Wiederherstellen in der T13-Oberfläche: `/tweaks deaths <player>` (Stufe `permission`, Standard 3) → Sicherung wählen
  (◀ ▶), Vorschau, „Alles wiederherstellen“ (ersetzt das Inventar; das aktuelle Inventar wird vorher selbst als
  Sicherung abgelegt, damit nichts verloren geht) oder einzelne Items herausnehmen (die Sicherung merkt sich das).
  Geht auch für Offline-Spieler (über die Offline-Spielerdaten aus T13).
- Tests: Tod legt Sicherung an (Inhalt, XP, Ursache), Obergrenze rotiert, Wiederherstellen online + offline.

## T13 /invsee und /endersee (Standard an)
- Eigene Oberfläche (eigene `MenuType`s + Screens im simplelib-Kasten, Palette `IRON` oben, helles Inventarfeld unten,
  Beschriftung `DARK_LABEL`): Rüstung (4) + Nebenhand oben, 27 Inventar + 9 Hotbar, darunter das eigene Inventar.
  Endersee: 27 Plätze. Admin nimmt/legt live (Container ist direkt das Inventar des Ziels; Shift-Klick in beide Richtungen;
  Rüstungsplätze nur passende Ausrüstung).
- Offline: `<world>/playerdata/<uuid>.dat` lesen (DataFixer wie Vanilla), Änderungen sofort atomar zurückschreiben
  (`Util.safeReplaceFile`, `.dat_old`). Lädt der Spieler gerade (Mixin `PlayerList.loadPlayerData`), wird jede offene
  Offline-Ansicht vorher geschlossen → kein Duplizieren. Namen → UUID über den Namens-Cache.
- Rechte: `permission` (Standard 2) zum Ansehen, `editPermission` (Standard 2) zum Ändern (sonst nur Ansicht).
  Kein invsee auf sich selbst.
- Tests: Ansicht spiegelt das Ziel live, Nehmen/Legen wirkt, Offline lesen/schreiben, Login schließt Offline-Ansicht,
  ohne Recht nur Ansicht. Kontrast-Test (JUnit) für alle Text/Hintergrund-Paare (≥ 4,5:1).

## T14 Vanish (Standard an)
- `/vanish [player]` (Stufe 2). Zustand bleibt über Relog/Neustart (`admin-state.json`).
- Unsichtbar für Nicht-Admins: `ServerPlayer.broadcastToPlayer` → false (Admins mit Stufe `seePermission` sehen weiter);
  beim Umschalten werden die Tracker neu ausgewertet. Tab-Liste: Info-Pakete an Nicht-Admins werden gefiltert,
  beim Umschalten Entfernen/Hinzufügen gesendet. Serverliste: Anzahl und Stichprobe ohne Unsichtbare.
  Keine Beitritts-/Austrittsnachricht (beim Umschalten auch keine gefälschte Nachricht – bewusst schlicht).
- Mobs: `canBeSeenByAnyone` → false. Sculk: Spiel-Ereignisse mit unsichtbarem Spieler als Quelle entfallen.
  Aufheben aus, außer `/vanish pickup` (je Spieler umschaltbar).
- Tests: Sichtbarkeit je Betrachter, Statusliste, Mob-Zielwahl, Vibration, Aufheben an/aus, Persistenz.

## T15 Freeze (Standard an)
- `/freeze <player>` / `/unfreeze <player>` (Stufe 2), überdauert Relog. Jeder Tick: Abweichung → Teleport zurück;
  Abbauen, Benutzen, Setzen, Angreifen, Fallenlassen werden verweigert. Hinweis: Titel beim Einfrieren, alle 2 s Aktionsleiste.
  Admins mit höherer Stufe als der Ausführende können nicht eingefroren werden (kein Aussperren von Ownern).
- Tests: Bewegung wird zurückgesetzt, Abbau verweigert, Auftauen gibt frei.

## T16 Neustart-Ankündigung (Standard an)
- Config `times` (`"HH:mm"`, Serverzeit), `warnMinutes` (Standard 15,10,5,1), `exitCode` (Standard 0, 0–255).
  `/tweaks restart <minutes>` und `/tweaks restart cancel` (Stufe 3).
- Countdown: Chat zu den Warnzeiten und bei 30/10…1 s, Bossleiste mit Restzeit. Bei 0: `saveEverything`, `halt`.
  Exit-Code ≠ 0 nur auf dedizierten Servern: ein Nicht-Daemon-Thread wartet auf das Ende des Server-Threads und ruft
  dann `System.exit(code)` (kein Deadlock mit dem Vanilla-Shutdown-Hook). Den eigentlichen Neustart macht der Wrapper.
- Tests: Planer berechnet nächsten Termin, Warnstufen feuern genau einmal, Abbruch, Stop-Aktion (im Test ersetzt).

## T17 Auto-Backups (Standard an)
- Alle `intervalHours` (Standard 6, 1–168) und per `/backup now` (Stufe 3): save-off (`noSave` je Level) →
  `saveEverything(flush)` → ZIP der Welt auf eigenem Thread (ohne `session.lock`, ohne Zielordner) → save-on.
  Ziel `directory` (Standard `backups`, relativ zum Serverordner), `keep` (Standard 5, 1–100) neueste Archive.
  Zeitpunkt des letzten Backups in `<ziel>/.simpletweaks-last`. Nie zwei Läufe gleichzeitig.
- Tests: Backup erzeugt ZIP mit `level.dat`, Rotation behält `keep`, save-on danach wieder an.

## T18 Wartungsmodus (Schalter an, Modus selbst inaktiv)
- `/maintenance on|off|status` (Stufe 3), Zustand in `admin-state.json`. Aktiv: Login nur für OPs ≥ `joinPermission`
  (Standard 2) und Namen/UUIDs in `allowed`; Kick-Text `kickMessage`, Serverliste-MOTD `motd`; beim Einschalten werden
  Online-Nicht-Admins getrennt (`kickOnline`, Standard an).
- Tests: Login-Prüfung, MOTD, Ein/Aus, Persistenz.

## T34 Lag-Finder (Standard an – Besitzer 17.10.: Features, die nur per Befehl wirken, sind an)
- `/lag [seconds]` (Stufe 2, Standard 5 s, 1–30): misst über das Fenster Tickzeit (MSPT/TPS), Zeit je Blockentity-Typ
  und Entity-Typ, Top-Chunks nach Entities/Blockentities, Redstone-Zustandswechsel je Chunk (Hinweis „mögliche
  Redstone-Uhr“ ab `clockThreshold` Wechseln/s). Messung nur während des Fensters (sonst ein Boolean-Check).
- Tests: Messung liefert Chunks/Typen, Redstone-Uhr wird erkannt, Schalter aus = kein Befehl.

## Doku, Prüfungen
- `modules/simpletweaks/wiki/manual.json`: je Feature ein Eintrag (EN/DE, Quellen); `docs/MODS-UND-FEATURES.md`.
- Discover-Tipps: nicht passend (keine Items; Tipps hängen an Item-Aufnahme) – bewusst keine.
- Regenerieren nur auf sb-test: `tools/discover/discover.py`, `tools/guides/module_guides.py`, `simplemoney/tools/generate_links.py`,
  `wiki/generate.py --all`; dann `checkModuleGuides checkModuleData checkWiki checkDiscover checkMultimod`.
- Kompilieren: 26.3 Fabric/NeoForge/Forge (Modul) + 26.2 `:compileJava`; GameTests `module-simpletweaks-*-263` +
  Standalone; Client-Screenshot der invsee-UI, falls der Harness es erlaubt.

## Risiken
- Mixin-Ziele unterscheiden sich je Loader (NeoForge/Forge patchen `ServerPlayer`, `PlayerList`, `Level`) → GameTests auf
  allen drei Loadern; Lag-Mixins `require=0` (wirken nur während einer Messung).
- Unsichtbarkeit ist kein Anti-Cheat (Paket-Ebene: Tracker + Tab-Liste + Status); Mods, die eigene Spielerlisten senden, sehen den Spieler.
- Backups großer Welten kosten Plattenplatz (`keep`, `intervalHours`), Rollback lädt Chunks synchron.
