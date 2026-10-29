# Wunsch „Simple Challenges“ (Besitzer 29.10.2026, nachts) – Spielmodi-Sub-Mod

Status: nur festgehalten. Später: INTENSIVE Recherche (beliebteste Mods/Datapacks je Modus: Chunk Unlock, Block Unlock, Skyblock, One Block, Lucky Block, „Block-Linie“-Challenges aus YouTube) – ansehen, wie sie es lösen, welche Probleme sie hatten, Code/Verhalten nachvollziehen (reverse engineering), dann NICHT kopieren, sondern verbessern und bündeln. Danach ausgefeilter Plan + Konzept-Artifact zur Abnahme. Umsetzung spätestens Phase 3; Konzept darf früher fertig bereitliegen.

## Ziel
- Sub-Mod (vermutlich unter Simple Tweaks/Server-Tweaks – Ort klären) mit vielen kleinen Challenge-Modi an EINEM Ort, für Creator und Casual-Spieler mit Freunden – ohne Mod-Suche, ohne Modding-Kenntnisse; im Modpack des Besitzers oder einzeln.
- Admin konfiguriert, welche Modi an sind. Modi kombinierbar (außer Chunk- vs. Block-Unlock = eine Einstellung im selben Modus).
- SICHERHEITSSPERRE: Ist die Mod installiert, aber nichts konfiguriert/aktiviert, ändert sie NICHTS am Spiel (harte Sperre als Notfallschutz). Einzelspieler: Weltgenerierung wird nur umgeschaltet, wenn eine Challenge gewählt wurde.

## Modi
1. **Chunk/Block Unlock**: Welt nur in freigeschalteten Bereichen begehbar (Grenze um den Bereich). Granularität als Zahl/Slider: jeder Block (schwerst) … jeder Chunk … alle 3 Chunks (leicht).
   - Startbereich konfigurierbar: Rechtecke über X/Z (z. B. x0–3, z0–7 plus x0–7, z0–3); Server vereinigt die Rechtecke, Konfig-Bildschirm zeigt die Form auf einer Karte. Mit Seed: Vorschau der Welt.
   - Spawn fest auf 0,0 ohne Streuung, innerhalb des freien Bereichs. Mod kennt die Blöcke unter dem Spieler und bestimmt daraus den Preis.
   - Preise „intelligent“: leicht = am Anfang Erde; spawnt man an einem Baum mit Holzzugang, auch Holz. Block-Unlock braucht schlauere Preislogik nach Weltgenerierung/Spawn.
   - Variante Erfolge: Erfolge geben Credits; Blick auf einen angrenzenden gesperrten Chunk zeigt Credits und ob man ihn freischalten kann; Freischalten kostet 1 Credit.
2. **Skyblock** (kompatibel mit Unlock, z. B. jede Seite/jeden Block der Insel freischalten): klassisches Skyblock, One Block, neuere Varianten – eine Kategorie. Vorschau des Spawns beim Auswählen.
3. **Lucky Block**: Block abbauen → zufälliger Block; häufige Blöcke wahrscheinlicher als seltene.
4. **Skyline Block** (Arbeitsname, „Sky“ + „Line“): Blöcke einzeln in der Luft mit einem Block Abstand.
   - Linear: ab 0,0 in +X auf einer Linie alle Overworld-Blöcke in fester Reihenfolge (z. B. Gras, Erde, Sand, Sandstein …), zuletzt Obsidian; im Nether alle Nether-Blöcke auf einer Linie, im End alle End-Blöcke (inkl. Shulkerkiste …). Abgebaute Blöcke kommen sofort nach (unendlich). Planung nötig, weil die Wege lang sind. Idee Besitzer: Ende Grundgestein, spezielle Spitzhacke → abbauen öffnet Endportal (Original recherchieren, anpassen).
   - Nicht tragfähige Blöcke (Rahmen, Pflanzen …) stehen auf einem Grundgestein-Stützblock darunter (keine Block-Update-Lags).
   - End: direkt Drachenkampf, aber keine Plattform, nur die Blöcke.
   - Raster-Variante: im positiven Bereich jede zweite Koordinate (0,0 → 2,0 → 2,2 → 0,2 → 4,0 → 4,2 …).
   - Ring-Variante: um 0,0 in Schichten nach außen (2,0 → 2,2 → 0,2 → −2,2 → −2,0 → −2,−2 → 0,−2 …).
   - EASTER EGG (linear + Raster, nicht Ring): in −X, wo keine Blöcke sind, in fester Entfernung je Dimension eine Vanilla-Struktur mit gutem Loot: End = Endschiff (Elytra), Nether = Mini-Bastion mit garantiertem Schmiedevorlage, Overworld = präparierter Tresor + Schlüssel (garantiert Heavy Core). Elytra und Schlüssel respawnen unendlich (wie die Linienblöcke); der Tresor existiert einmal, jeder Spieler kann ihn einmal öffnen. Je Easter Egg eine Backup-Struktur in gleicher Entfernung (2 Strukturen insgesamt).
5. Weitere Modi: Agent schlägt vor (Ziel ~16 Modi, mehr oder weniger ok).

## Presets mit Namen
Grundmodi als Presets mit coolen Namen, z. B. „Vanilla Unlock“ (Chunk-Unlock je Chunk), „Vanilla Block Unlock“ (je Block, schwer), „Skyblock Unlock“, „Lucky Block Unlock“, „Skyline Block“. Eigene Presets anlegen/speichern.

## Welt-Erstellung (UI)
- Neuer Welttyp/Tab neben Superflat usw.: „Simple Challenges“ (oder cooler Name).
- Darunter drei Knöpfe mit den zuletzt gespielten Challenges (z. B. Skyblock, Sky One Block, Skyline Block) und „Select Challenge“.
- „Select Challenge“ öffnet einen Bildschirm wie die Mod-Konfiguration: Presets wählen/anlegen; Auswahl mit weißem Rand wie Minecrafts Standard-Auswahl; „Play Challenge“/„Select“ legt die Challenge für die Welt fest; „Create New World“ erstellt sie.
- Im UI-Stil des Besitzers (einheitlich, siehe UI28).
