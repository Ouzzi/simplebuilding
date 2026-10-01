# Vereinte Feature-Welle – 2026-10-01

Ausgang `821dd131`, vereinter Quellstand `5f294df9`. Plan:
`docs/ai/CODEX-PLAN.md`. Claims bleiben AUS; offene Aktivierungsblocker stehen in
`modules/simpletweaks/CLAIMS-STAGE4.md`. Forge wird separat weitergeführt.

- Claims 1–4: Merge `bbfc39c6`, Worker `71753fe2`; Zugriffe/Portale 5–6:
  Merge `dc147181`, Worker `2a878f7a`. Beide Worker-checks Exit 0.
- Dimensions-Einstellungen: Merge `5bb8c442`, Worker `8657e3d2`; UI-Test:
  Merge `295ad36a`, Worker `385e4e57`. UI-Test nur kompiliert.
- QoL/Sounds: Merge `6bb478cd`, Worker `7faca095`, Worker-check Exit 0.
- Gemeinsame komplette Tweaks-/Dimensions-Kataloge auf Fabric und NeoForge:
  **alles gruen: 176/176 bestanden, 0 rot**, Run
  `2026-10-01T00-27-20Z-0efa`, Gate-Worktree auf `5f294df9`.
  Tweaks 47 je Loader, Dimensions 41 je Loader. Ergebniszeile gelesen.
- Zehn Produktions-JARs gebaut (`JAR_EXIT=0`) und verschachtelte Archive geprüft:
  SimpleBuilding, Tweaks, Dimensions, Sounds und Visuals, jeweils Fabric/NeoForge.
  Alle enthalten genau eine Framework-0.1.1-JAR mit `Protection.class` und
  `CosmeticIntensity.class`. Beide SimpleBuilding-JARs enthalten außerdem
  `FrameworkProtection.class` und den passenden ServiceLoader-Eintrag.
- Acht geänderte Sprachdateien: keine doppelten JSON-Schlüssel, EN/DE-Schlüssel
  je Modul identisch. Merge-Prüfungen für Manifest, Wiki, Quests, Wiki-Tests,
  Bücher und Texturen grün. Beide Claims-/Dimensions-Testkataloge erhalten.
- Unabhängige rein statische Review auf `5f294df9`: keine zusätzlichen konkret
  belegten Defekte im vereinbarten Umfang. Keine eigene Ausführung durch Reviewer.
- Nicht geprüft: tatsächlicher Dimensions-Dialog, Client-Audio/Visuals,
  Besitzerwelt und andere Minecraft-Laufzeitlinien. Besitzer-Client läuft;
  keine Clients gestartet oder gestoppt. Claims sind nicht vollständig abgesichert.

Das vollständige Gate mit Hauptlinie und allen Integrations-/Modulsuiten sowie
der Push erfordern einen eigenen grünen Lauf auf der endgültig gewählten SHA.
Einzel- oder Modulprüfungen ersetzen diesen Nachweis nicht.

## Gesamtgate und Korrektur der Testisolation

Gate auf `354b1ae3`: `check: OK`, Hauptlinie **1600/1600** grün
(`2026-10-01T00-53-59Z-55d1`), Integration **ROT**
(`2026-10-01T00-59-50Z-bcf2`, 38 Portalfehler). Kein Push.

Der Minecraft-26.3-Bytecode belegt: `GameTestHelper.runBeforeTestEnd` plant
bei `getTimeoutTicks() - 1`; es ist kein Abschluss-Callback. Erfolgreiche neue
Settings-Tests ließen alle drei Dimensions-Schalter auf der Platte ausgeschaltet.
Die nächste Serverinstanz las diesen Zustand korrekt und verweigerte Portale.
`ClaimPortalTests` verwendete denselben falschen Cleanup-Ansatz.

Fix `a5d8151e` räumt bei Erfolg und Fehler explizit und idempotent auf; der
Timeout-Fallback bleibt. Alle Test-IDs, Gameplay-Assertions und Verzögerungen
erhalten. Zwei komplette Tweaks-/Dimensions-Läufe nacheinander, ohne Rücksetzen
zwischen den Läufen: **176/176** (`2026-10-01T01-19-36Z-79f7`) und **176/176**
(`2026-10-01T01-23-28Z-ec4c`). Beide `server.json`-Dateien waren nach jedem Lauf
bytegleich zum Ausgangszustand. Die vorher beschädigten Wegwerf-Konfigurationen
wurden vor einmaliger Reparatur gesichert; Besitzerdateien blieben unberührt.

Die Forge-Prüfung fand außerdem eine zeitliche Testkollision. Deren getrennte
Settings-Testumgebung wird vor dem abschließenden Gesamtgate übernommen.

## Produktions-Bootstrap

Die beiden normalen 26.3-JARs enthielten zunächst keinen der vier gemeinsamen
Bootstrap-Typen. Die isolierte Gegenprobe mit einem URLClassLoader ohne
Projekt-Classpath scheiterte beim alten Fabric-JAR mit
`ClassNotFoundException: com.simplebuilding.common.SimplebuildingBootstrap`.
Fix `f23d1004` nimmt den bestehenden `:common`-Output in beide JARs auf.
Nach erneutem Build bestanden beide Archive denselben isolierten Aufruf von
`SimplebuildingBootstrap.initialize`, einschließlich seiner inneren Builder-
Abhängigkeiten und der einmaligen Initialisierung. Kein Minecraft-Client gestartet.

## Forge-Nachprüfung vor dem Merge

Die Ergebniszeilen der Worker-Protokolle wurden separat gelesen:
- Vollständige betroffene Kataloge auf Fabric, NeoForge und Forge: **264/264**,
  `2026-10-01T02-03-44Z-90ee`. Die beiden Cleanup-Dateien entsprechen exakt
  `a5d8151e`; Settings laufen zusätzlich in einer eigenen Testumgebung.
- Unmittelbare Forge-Wiederholung: **88/88**, `2026-10-01T02-11-47Z-315a`.
  Die vier Testkonfigurationen bleiben laut Bytevergleich unverändert.
- Sounds mit installiertem Visuals: **17/17**, `2026-10-01T02-13-59Z-1192`.
- Normaler Forge-Entwicklungsstart, vier Claims-Fälle: **4/4**,
  `2026-10-01T02-15-19Z-efa1`; zusätzlich zu den produktiven JAR-Starts.
- Testzentralen-Neubau und vollständige SimpleBuilding-Item-/Blockabdeckung in
  isolierten Fabric-/NeoForge-Welten: **10/10**, `2026-10-01T02-16-20Z-36d6`.

Diese Einzelbelege ersetzen weiterhin nicht das gemeinsame Abschlussgate.

Forge-Worker `6786b545` ist mit `39fd85b1` konfliktfrei integriert. Beide Worker-
Checks (Default und `-Pforge263=true`) endeten mit Exit 0; die vollständigen
Forge-Kataloge belegen zusammen 1.039 unterschiedliche Fälle. Dateiinventar,
Run-IDs, Artefaktprüfsummen und Grenzen: `docs/FORGE-FOLLOWUP-RESULTS.md`.
Beim Merge waren Manifest, Wiki, Quests, Wiki-Tests, Bücher und Texturen grün.
Das gemeinsame Abschlussgate wird gegen den nachfolgenden Dokumentationscommit
ausgeführt; SHA und VERDICT stehen in `.ai-runs/final-full-gate.log` und im
Abschlussbericht. Nur diese SHA darf nach GREEN gepusht werden.
