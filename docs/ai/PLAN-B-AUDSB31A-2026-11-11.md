# Plan b-audsb31-a (B, 11.11.2026) – Code-Audit Storage, Machines, Pads, Gadgets

Quelle: /root/sb-tools-b/relsb31-lücken.md Punkt #9. Basis `hub/b-bundle182` 9b5764995 (kein `-gen` vorhanden).
Vorlage: `modules/simplemodels/AUDIT.md` u. a. (b-rel32, 31.10.). Nur MC 26.3 Fabric.

## Ist-Stand
- Keine `AUDIT.md` in den vier Modulen. `docs/ai/AUDIT-A-2026-11-08.md` (hub/a-audit09) ist ein Lücken-/Feature-Audit
  (JEI-Keys, Dubletten, Handbuch), keine Sicherheitsprüfung → wird nur verlinkt, nicht wiederholt.
- Umfang (ohne Forge/NeoForge): Storage 30,6k Zeilen/167 Dateien, Pads 13,6k/93, Machines 12,1k/70, Gadgets 11,6k/54
  (davon ~45 % Tests). Ein Vollaudit Zeile für Zeile ist in einem Lauf nicht machbar → **risikogeleitet**.

## Vorgehen
1. Je Mod gezielt lesen (vollständig): alle C2S-Payloads + Handler, alle Menüs (quickMoveStack, clickMenuButton,
   stillValid, Slot-Regeln), alle BlockEntities (load/save, Tick, Container), Items mit Komponenten-Inhalt
   (Rucksack/Bündel/Köcher/Shulker: Verschachtelung, Größe), Befehle, Teleport/Chunkload, Schutz (Claims/mayInteract).
2. Befunde P1/P2/P3 je Mod in `modules/<mod>/AUDIT.md` (Aufbau wie simplemodels: gelesene Dateien, C1–C9, Befunde, Urteil, Test).
3. Kleine sichere Fixes direkt, je Fix ein Commit + Gametest (Katalog + @GameTest im Fabric-Adapter), größere als Liste.
4. Lokal: javac der geänderten Dateien gegen 26.3-Klassenpfad, `precheck --fast`, `modules/*/tools/check_data.py`,
   check_dup_impl, check_tooltip_text.
5. EIN sbt am Ende: Compile + module-<mod>-fabric-263 + standalone für die 4 Mods + data_integrity.

## Risiken
- Dubletten Kern↔Modul (check_dup_impl): Fix nur im Modul; Kern-Kopie ggf. als Befund notieren.
- Verhaltensänderungen an Spielmechanik = Besitzer-Entscheid → nur notieren.

## Ergebnis
| Mod | P1 | P2 | P3 | Fixes (Commit) |
|---|---|---|---|---|
| Storage | 0 | 2 (gefixt) | 4 | d1fa2fb1b Rohr fragt `canTakeItem`; 77bf250aa + fa61307b6 Controller-Schift-Klick verliert nichts |
| Machines | 0 | 1 (gefixt) | 2 (1 gefixt) | 056a68ad9 Doppelkolben räumt zerstörte Zellen wie Vanilla; 469b075a4 Kolben-Regeln beim Trennen vergessen |
| Pads | **1 (gefixt)** | 1 (Rückfrage) | 2 | 56ba7d09c verwaister Flypad-Flug wird alle 20 Ticks entzogen (+ b9a56b426 dup_pairs) |
| Gadgets | 0 | 0 | 2 | – |

Details je Mod in `modules/<mod>/AUDIT.md`. Neue Gametests: `pipe_pull_respects_can_take_item`,
`network_shift_take_with_full_inventory_loses_nothing`, `double_piston_breaks_a_pot_with_its_item`, `stale_flypad_flight_is_swept_away`.

## Abweichungen
- Kein Zeile-für-Zeile-Vollaudit (68k Zeilen); risikogeleitet, gelesene Dateien je AUDIT.md aufgeführt. Nicht gelesen: Screens/Clients,
  Fahrzeuge, Spawn-Teleporter, Pad-Physik, Rod-Strahl im Detail. Kandidat für eine Nachrunde, falls gewünscht.
- Flypad-Fix nur im Modul; die 26.2-Kern-Kopie (auf 26.3 nicht registriert) hat denselben Fehler → Drift in `tools/dup_pairs.txt`
  akzeptiert, beim 26.2-Port nachziehen.
- simplemachines-P3-1 (Client-Trennen) ohne Gametest (reiner Client-Pfad).
- Selbst entschieden: Controller-Rest wird **abgeworfen** statt den Schift-Klick abzulehnen (Vanilla-Verhalten bei vollem Inventar).

## Rückfragen (Besitzer)
- **AUDSB31-F1** (simplepads-P2-1): Chunk-Loader ohne Limit je Spieler/Server. Soll es eine Server-Option `chunkLoaderMaxPerPlayer`
  geben? Vorschlag A: ja, Standard 4 (Stufe egal), Operatoren ausgenommen. B: ja, Standard 0 = unbegrenzt (nur Option). C: nein, bleibt
  (Besitzer-online-Regel reicht).

## Test
Lokal: javac der geänderten Dateien (nur bekannte Access-Widener-Meldungen), `precheck --fast` grün, 4x `check_data.py` grün,
`check_dup_impl` 0 Probleme, `check_tooltip_text` grün. sbt (ein Lauf): siehe unten.
