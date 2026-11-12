# Plan a-audsb31b (A, 12.11.2026) – Besitzer-Antworten AUDSB31B F1–F9 umsetzen

Quelle: `/root/sb-tools-a/agenten-1530.md` 15f, Fragen `/root/sb-tools-a/previews/a-audsb31-preview.html`,
Antwort `/root/sb-tools-a/abnahme-a/antworten.md` (12.11. ~22:20 UTC): **AUDSB31B=ok (alle Empfehlungen F1–F9)**.
Basis `hub/b-bundle190` (43b9d09bd), Ziel Bündel 191. Nur 26.3 Fabric. Hintergrund: `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`,
`modules/<mod>/AUDIT.md`.

Gewählt (= Empfehlung): F1=A, F2=B, F3=A, F4=A, F5=A, F6=B, F7=A, F8=A, F9=B.

## Ist-Stand und Plan je Frage

| F | Mod | Ist (Code) | Änderung | Test (Gametest) | Texte |
|---|---|---|---|---|---|
| F1 | simplechisels | `ChiselItem#isCorrectToolForDrops` = jeder Block aus `mineable/pickaxe|axe|shovel`, ohne Stufe; Tempo hängt daran (sonst 1.0) | Drops nur, wenn der Block nicht in `tier.material.incorrectBlocksForDrops()` liegt (wie Vanillas Werkzeug-Komponente: `deniesDrops` + `minesAndDrops`); Tempo bleibt an „abbaubar“ (Vanilla: falsche Stufe = Materialtempo, aber /100 statt /30) | `chisel_tier_decides_drops`: Stein-Meißel kein Diamanterz/Obsidian, Eisen ja Diamanterz, Diamant ja Obsidian; Stein weiter Stein/Eisenerz | Wiki-Details EN/DE, Guide (Summary unverändert) |
| F2 | simplebuildingtools | `blueprint.maxBlocksPerTick` bis 32 768 (Standard), keine Rechteprüfung für die Größe | neue Server-Optionen `blueprint.largeBuildCells` (Standard 32 768, 0 = aus) und `blueprint.largeBuildPermission` (Standard 2 = OP): ein **Gratis-Bau** (Kreativ / Kreativ-Stab) mit mehr Stellen braucht die Rechtestufe; Tempo bleibt | `large_free_build_needs_operator`: Kreativ-Mock ohne OP, Grenze klein gesetzt → abgelehnt; Grenze 0 → baut | Config-Lang EN/DE, `check_data.py` KEYS, Wiki-Config-Zeile EN/DE, Ablehnungsmeldung EN/DE |
| F3 | simplefoundry | Pult-Stufe nur im BlockEntity (`Tier`), Loot-Tabelle ohne Komponenten | Komponente `simplefoundry:desk_tier`; `collectImplicitComponents`/`applyImplicitComponents`/`removeComponentsFromTag` im Pult, Loot-Tabelle `copy_components` (block_entity); eigenes `DeskItem` mit Stufen-Zeile im Tooltip (Tooltip-Regel b: Stufe sonst unsichtbar) | `desk_tier_stays_on_the_item`: Pult auf Stufe 3, Drops über die Loot-Tabelle → Item trägt Stufe; Item auf neues Pult → Stufe 3; Stein-Pult ohne Komponente | Tooltip EN/DE, Wiki/Guide EN/DE (Stufe bleibt; Schmelze/Brennstoff gehen beim Abbau verloren – plan40 simplefoundry-1) |
| F4 | simplearrows | Lohenrute: `fire + 60` je Treffer ohne Grenze | `max(fire, min(200, fire + 60))` (10 s Obergrenze, längeres Feuer z. B. Lava wird nie gekürzt) | `blaze_shaft_fire_capped`: 10 Treffer → ≤ 200 Ticks, 1 Treffer → 60 | Wiki EN/DE (Schäfte-Zeile) |
| F5 | simplearrows | Windstoß der Böenrute schiebt auch Gegenstände/Rüstungsständer (Pfeil als Quelle) | **keine Code-Änderung** (A = so lassen) | im bestehenden Test `impact_effects_happen_on_a_direct_hit`: Gegenstand neben dem Ziel wird geschoben (nur bei `mobGriefing` an, wie Vanilla-Regel) | Wiki EN/DE: „schiebt auch Gegenstände und Rüstungsständer“ |
| F6 | simplebuildingtools | `copyResult` macht aus jeder leeren Blaupause eine Kreativ-Kopie | `copyResult(original, blank, player)`: Kreativ-Blaupause nur kopierbar, wenn der Spieler im Kreativmodus ist; sonst kein Ergebnis | bestehender Test `creativeBlueprintBuildsFarAndStaysLockedOnceSigned` erweitert: Überleben → leer, Kreativ → genaue Kopie | Wiki EN/DE |
| F7 | simplemobs | Fakes (Tag `simplemobs_fake`) geben Vanilla-EP | Mixin `DeceiverFakeXpMixin` auf `LivingEntity#getExperienceReward` (final, keine Overrides): Fake → 0; echte Beschworene unverändert; hält über Neustart (Tag wird gespeichert) | `deceiver_fakes_give_no_xp`: Zombie mit Tag 0 EP, ohne Tag > 0 | Wiki EN/DE |
| F8 | simplemobheads | `wearerTick` setzt Größe 1,5× ohne Platzprüfung | Größe (und Stufenhöhe) nur, solange die stehende Box in voller Größe frei ist; im engen Gang fällt sie weg, im Freien kommt sie zurück; Tempo des Riesenkopfs bleibt | `giant_head_needs_room`: Decke auf 2 Block Höhe → 1,0×; Decke weg → 1,5× | Wiki/Guide EN/DE |
| F9 | simpleresonance | „bewegt“ nur über Gehen/Sprint/Schleichen/Fliegen/Klettern (`totalDistance`) | neues `movedDistance` = `totalDistance` + Wasser (an/unter/schwimmen), Elytra, Lore, Boot, Schwein, Pferd, Schreiter, Glücklicher Ghast, Nautilus – nur für „bewegt“; Wegstrecken-Formel (`totalDistance`) bleibt | `riding_counts_as_moved`: jede Statistik macht wieder aktiv; `totalDistance` bleibt gleich | Wiki EN/DE (aktive Zeit) |

Gemeinsam: je Mod `AUDIT.md` (Rückfrage → „Besitzer AUDSB31B Fx = …, umgesetzt“), Guides neu (`tools/guides/module_guides.py`),
Fabric-Adapter `@GameTest` je neuem Test, `docs/MODS-UND-FEATURES.md` nur falls dort die geänderte Regel steht.

## Risiken
- Kern-Dublette Meißel (`common/.../ChiselItem.java`, `tools/dup_pairs.txt`): Fix nur im Modul, Drift in `dup_pairs` vermerken (26.2-Port).
- F2 Abweichungsgefahr: Antwort sagt „Bauten über 32 768 Stellen nur für OPs“; Frage und Audit begründen nur Gratis-Bauten
  (Überleben begrenzt das Material). Umsetzung gilt für Gratis-Bauten – als Abweichung benannt.
- F3: neue Datenkomponente → Registrierung vor Item-Nutzung (Fabric-Init); NeoForge/Forge nicht im Umfang (nur 26.3 Fabric).
- Überschneidung mit a-w1a (buildingtools), a-w1c (chisels/resonance), a-w1e (mobheads), a-w1b (mobs): vor Push `git fetch hub`, Diff prüfen.
- sb-test nicht erreichbar → nur javac lokal; sbt-Befehl als Datei + TODO.

## Prüfen
javac aller geänderten Dateien je Modul (`/root/sb-tools-a/audsb31b/javac.sh`), `precheck.py --fast`, alle `check_data.py`,
`check_dup_impl`, `check_tooltip_text`, `check_gametest_adapters`, `wiki/generate.py --all --check`, `check_missing_textures`,
`layers.py check`, `module_guides.py --check`. sbt (wenn erreichbar): Modul-Gametests fabric-263 + standalone für
chisels, buildingtools, foundry, arrows, mobs, mobheads, resonance.

## Ergebnis
(nach Umsetzung)
