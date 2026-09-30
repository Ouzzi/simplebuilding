---
name: offene-mod-befunde-2026-09
description: Was von den Mod-Befunden behoben ist; seit 2026-09-24 auch die 27 P4-Defekte und isDestroying
metadata:
  node_type: memory
  type: project
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-24T12:00:00.000Z
---

**Behoben am 2026-09-08 (Commit `257446d`), jeder durch seine eigene Gegenprobe belegt:**
Ofen-Menütitel je Stufe; Pfeilfilter folgt der Klickbelegung; Unendlichkeit kostet keinen
Köcher-Pfeil (über `EnchantmentHelper.processAmmoUse`); Vorschau und Abbau des Adernabbaus fragen
eine Erzliste; Trichterfilter sitzt im Slot statt in einem Klickzweig; ungültiger `FilterMode`
wird geklemmt statt die Blockentität zu verwerfen; `QuiverItem`-Parameter verdeckte seinen Typ.
Zuvor behoben (`d149f08`): try/finally beim Adernabbau, dynamisches Licht im Wasser,
Schwimmgeschwindigkeit im Besatz.

**Alle sieben Entscheidungen sind getroffen und umgesetzt** (Commit `61d0a14`): Enderit ist eine
vollwertige Stufe (Explosionsschutz + alle drei Verzauberbarkeits-Tags), der Köcher-Brustslot ist
fertiggebaut (`EQUIPPABLE` für `EquipmentSlot.CHEST`), der Erzsucher ist verhaltensneutral
entlastet, und die drei Doppelpflegen sind zusammengelegt.

**Nebenwirkung, dokumentiert statt weggepatcht:** `SmithingScreenHandlerMixin.isValidArmor` fragt
„trimmbare Rüstung **oder** irgendetwas Anlegbares". Köcher und Netherit-Köcher erfüllen das seit
der `EQUIPPABLE`-Komponente, sind also gültige Basis für Glowing- und Emitting-Trim. Der
Emitting-Trim **wirkt wirklich** — `DynamicLightHandler` summiert den Brustslot mit, ein getragener
Köcher leuchtet. Der Enderit-Köcher ist nicht betroffen: er ist in keinem Schmiederezept `base`.

**Die teuerste Lektion aus diesem Block:** Ein Fix am Datagen-*Anbieter* erreicht das Spiel nicht.
Der Server lädt die committeten Dateien unter `src/main/generated`; ohne `runDatagen` (beide
Linien!) ändert sich nichts, und nichts sagt es. Genau das ist zweimal an einem Tag passiert.

**Ungeklärt, weil keine Quelle es entscheidet: der Drawer-Faktor.** Bis 2025-12-29 standen Code und
Kommentar auf `(8 + level) / 8`; Commit `ce9f495` hat beide Aufrufstellen wortlos auf 16 gestellt.
Das Handbuch nennt 16, ist aber *beschreibend* — es sähe bei einem Ausrutscher genauso aus. Die
Zahlen sind festgenagelt, damit sie sich nicht ein zweites Mal unbemerkt bewegen. Siehe
[[kommentar-ist-kein-beweis]].

**Behoben am 2026-09-24: die Riss-Vorschau hing an `isDestroying`** (Befund aus einer
Client-Testspur, 2026-09-10). Vanilla baut im `sameDestroyTarget`-Zweig von `continueDestroyBlock`
ohne das Flag weiter (gleicher Block + gleiches Werkzeug nach `stopDestroyBlock`, z. B. Fadenkreuz
einen Tick neben dem Block). Der naive Fix (nur `getDestroyStage()` in 0..9) wäre eine Regression:
nach so einem Abbau bleibt die Stufe stehen, `stopDestroyBlock` ist ohne Flag ein No-op, und die
Mod zeichnete Risse um jeden anvisierten Block. Umgesetzt: Stufen-Check **plus** „Vanilla hat an
`mainPos` selbst einen Riss extrahiert" (`vanillaCracks`). Client-Testfall mit Gegenprobe gegen
veraltete Stufen; zwei P9-Mutationen.

**Alle 27 P4-„Entscheidung des Besitzers"-Einträge am 2026-09-24 aufgelöst** (Stand je Eintrag in
`testing/audit_offen.json` unter `p4.stand`, Übersicht am Ende von `testing/P4-TRIAGE-2026-09-10.md`).
Der Besitzer hatte „bis alles erledigt ist" gesagt; zwei Punkte wurden **nicht** geändert, sondern
als Regel dokumentiert: negativer Baustab-Radius (baut wie die Vorschau den Mittelblock — kein
Defekt) und erzwungene Achse (Ebene bleibt um den Block vor der Seite zentriert, die geklickte
Zelle fällt weg — so steht es im Handbuch und in `ItemBehaviourTests`). Forge-Punkte nur im Code
angeglichen (Loot-Funktion, Constructor's Touch, Luftsprung, Befehl), Forge bleibt ungestartet.
Neue Gegenproben: Katalog `P9_MUTATIONS` (`mutations.py --p9`).

Rohdaten: `testing/audit_falsegreens.json`, `testing/audit_offen.json`,
Urteile im Scratchpad unter `modbefunde_urteile.json`.

Siehe auch [[testabdeckung-2026-09]] und [[gametest-harness-fallen]].

**Behoben am 2026-09-10 (Nachmittag), aus dem sauberen Vier-Ziele-Lauf und der P6-Runde:**
- Die drei HUD-Overlays (Luftsprung-Balken, Tacho, Rangefinder) ignorierten **F1 auf NeoForge**:
  `RegisterGuiLayersEvent`-Ebenen bekommen keinen `hudVisible`-Schalter, Fabrics
  `HudElementRegistry` hängt das Element in eine Vanilla-Ebene und erbt ihn. Jetzt fragt das
  geteilte Overlay selbst (26.2 `client.gui.hud.isHidden()`, 1.21.11 `client.options.hideGui`);
  `AirJumpClientTest` verlangt den leeren Frame.
- `VeinMinerUsageEvent` trug eine **eigene Kopie** der Aderabbau-Stufentabelle und der
  Breitensuche; die Vorschau las `MiningUtils.getVeinMinerBlocks`. Eine P6-Mutation der Tabelle
  blieb grün. Jetzt fragt der Abbau dieselbe Methode (Auswahl identisch, vier Tests grün).
- `mc1_21_11/neoforge/build.gradle` prüfte noch `screenshots/RESULT.txt` des abgelösten
  RendererProofRun — jeder Lauf des geteilten Treibers „scheiterte" auf Gradle-Ebene; jetzt
  `client-test-result.txt` mit `PROVEN`.
