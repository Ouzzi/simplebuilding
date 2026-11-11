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

## Abweichungen / Ergebnis
(wird am Ende ergänzt)
