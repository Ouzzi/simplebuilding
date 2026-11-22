# Update 1 nach dem Erst-Release – Übersicht der vier Pläne (Sitzung B, 22.11., NUR PLAN)

> Status: **nur Planung vor dem Erst-Release** (O6-2, `/root/b-auftrag-20261109.md` Abschnitt „## O6“). Kein Bau, bis der
> Besitzer Update 1 freigibt. Update 1 = nur 26.3 Fabric (wie das Erst-Release).

| Plan | Inhalt | Branch | Pakete | Aufwand | Blockade vor Bau |
|---|---|---|---:|---|---|
| `PLAN-UPDATE1-N96B-SHEETS-2026-11-22.md` | mehrseitige Sheets, Nihilit-Sheet, N96C-Overlay, Blei 4/8 – **gebaut**, nur Rebase/Bündel | b-sheet96 b0a4eee99d | 5 | M (≈ 1,5–2,5 T) | Abnahme N96C |
| `PLAN-UPDATE1-N105-RECIPES-2026-11-22.md` | neuer Sub-Mod Simple Recipes (V2+V3, Z1 A1 F1 K1 J1) | b-recipes105 (neu) | 8 | L+ (≈ 8–11 T) | Name/Einordnung, Release, Standard-Stil |
| `PLAN-UPDATE1-N101-2026-11-22.md` | Interface-Rails (4 Stufen), Trompete, Ender-Herz, Gems, Autostripper-Stufen | b-n101 (neu) | 6 | M–L (≈ 5–7 T) | Rail-Modus, Ofen-Lore, Stripper-Ausgabe, Vorschau N101 |
| `PLAN-UPDATE1-N83-GOLEMS-REST-2026-11-22.md` | Golem-Modelle R3, Bronze-Dash, Pflaster-Pfade, Statuen/Köpfe (9 TZ-Felder), Plüsch | b-golem83 81a047d644 | 8 | L+ (≈ 9–12 T) | Abbau-Regel Pflaster, Holz-Varianten, Statue vs. Plüsch |

T = Agenten-Tage (S ≤ ½ T, M ≈ 1–2 T, L ≥ 3 T je Paket), ohne Wartezeit auf Besitzer-Abnahmen.
**Gesamt grob ≈ 24–33 Agenten-Tage**; mit zwei parallelen Spuren und seriellen sb-test-Läufen realistisch **≈ 3–4 Wochen**.

## Abhängigkeiten
- **N96B → N83 (hart):** beide ändern `simpleores` (Loader-Klassen `OresFabric`/`OresForge`/`OresNeoForge`, `gen_data.py`,
  `check_data.py`, `OresTests`, Lang, TZ-Layout `layout_lines_metals.json`). b-golem83 wird erst auf den Stand **nach** dem
  N96B-Merge gezogen, sonst zweimal Konflikte.
- **N83 → N101 (weich):** keine gemeinsamen Dateien; nur die Besitzer-Reihenfolge (N101 vor N83).
- **N105 ↔ N101:** keine gemeinsamen Dateien (neuer Mod vs. simplestorage/simpletools/simplemobs). Berührungspunkt nur
  RecipeViewer-Einträge: neue N101-Stationen (Stripper-Stufen, Rail) melden sich bei `RecipeViewer` an, Simple Recipes liest
  sie automatisch – Reihenfolge egal.
- **N105 ↔ simpleinterfaces/simplemodels:** Rezeptbuch-Mixins (Z1-Ausschluss) – nur N105 betroffen.
- Alle vier enden im **gemeinsamen Update-1-Bündel** (Gate, Dev-Klick, Besitzer-Freigabe, dann Production).

## Empfohlene Reihenfolge
1. **N96B** – fast fertig, kleinste Arbeit, entsperrt N83; nur N96C-Abnahme nötig.
2. **N105 Simple Recipes** – Besitzer-Priorität (19.11.: „nach N89C, VOR N101 und N83“); größter neuer Inhalt, früh starten.
3. **N101** – Besitzer-Reihenfolge vor N83; Vorschau N101 zuerst (Abnahme läuft, während gebaut wird).
4. **N83-Rest** – zuletzt (Besitzer-Reihenfolge), aber technisch gleich nach N96B möglich.

## Parallel unter der 2-Agenten-Grenze
Zwei Spuren, je **ein** Agent, keine Unter-Agenten; schwere Läufe auf sb-test über `/root/sbt` bzw. Gate-Skripte
(Sperre `/root/.heavy.lock`, nacheinander), Client-Proben fährt die Hauptsitzung einzeln.

| Spur | Agent 1 („simpleores-Spur“) | Agent 2 („neue Inhalte“) |
|---|---|---|
| 1 | N96B (P1–P5) | N105 P1–P3 (Modul, UI-Bausteine, Freischaltung) |
| 2 | N101 P1 Vorschau + P2–P4 (Stripper, Herz/Gems, Trompete) | N105 P4–P6 (Tabs/Suche, Rezeptansicht, Z1/Tasten) |
| 3 | N101 P5–P6 (Interface-Rail, Texte) | N105 P7–P8 (JEI-Brücke, Texte, N105B) |
| 4 | N83 P0–P7 (Rebase nach N96B, dann Bau) | frei für Abnahme-Nacharbeiten (N101/N105-Einwände) |

Begründung: Spur 1 hält alle simpleores-Änderungen in einer Hand (kein Merge-Streit), Spur 2 baut den neuen Mod isoliert.
Die Besitzer-Reihenfolge N105 → N101 → N83 bleibt beim **Start** gewahrt. Wenn der Besitzer N83 vorziehen will, tauscht
Agent 1 nach N96B einfach N101 gegen N83 (beides ohne gemeinsame Dateien mit N105).

## Für alle vier gemeinsam
- Je Vorhaben: Plan-Abgleich (Vorgabe Wort für Wort), Gametests normal + standalone, **eine** Probe-Datei
  `uiprobe_<task>.py` mit den Probe-Lehren aus `/root/sb-tools-b/mod-pass-template.md`, Dev-Klick `/root/n74dev/<task>_wiki.cjs`
  (ein Chromium-Lauf zugleich), bprep „BPREP GRUEN“, dann Bündel + Gate. Kein Express (Java/modules.json).
- Welt-Kompatibilität mit dem Erst-Release: keine Id wird umbenannt → keine IdMoves. Verhaltensänderungen an freigegebenen
  Blöcken (Bleiplatte 4 statt 8, Autostripper je Zug) in den Changelog.
- Production erst nach Besitzer-Freigabe (TODO `development` → Production).
