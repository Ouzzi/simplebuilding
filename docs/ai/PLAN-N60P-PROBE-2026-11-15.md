# N60P – ui_probe Vollauf grün machen (2026-11-15, Branch b-client60p)

Eingang: Vollauf `ui_probe.py` auf e275be61b (507/536 ok, 29 FAIL), Konsole, results.json, Screenshots, client.log
(sb-test `/var/tmp/sb/uiprobe60`). Vorgehen: jede FAIL-Zeile gegen Screenshot / Code / client.log geprüft, dann kleinste Änderung.
Ergebnis: **alle 29 FAIL sind Probe-Annahmen/Timing (a) oder gewolltes Verhalten (c); kein Mod-Code-Fehler gefunden.**

## Hauptursache Gruppe A (12 Checks): Fenster ragt über den Bildschirm
Session B startete den Vollauf mit `xvfb-run -a` ohne `-s` -> Debian-Vorgabe 1280x1024. Das Spielfenster öffnet mit 854x480
mittig (x≈213) und SortProbe vergrößert es auf 1280x720 von dieser Ecke aus -> rechter Teil (Fenster-x > ~1066) liegt außerhalb
des X-Bildschirms, XTest klemmt den Zeiger an den Rand, der Klick trifft etwas anderes. Beleg: alle roten Klicks liegen rechts
von x≈1066 (Configure 1252, Face 1195, File/Back oben rechts, Fragebogen-Pfeil rechts ≈1100), alle grünen links davon
(Cube ≈1063 ok, linker Pfeil ok, Mods-Knopf ok). Frühere grüne Läufe (w4c-g1b/g3b, gleicher Mod-Code) liefen mit `-screen 0 2560x1440x24`.
Fix: `ui_probe.py` startet bei zu kleinem Display ein eigenes Xvfb 2560x1440 und bricht ab, wenn das Fenster nach dem Weltstart
nicht in den Bildschirm passt (klare Meldung statt Folgefehler).

## Tabelle
| Check | Ursache | Beleg | Fix |
|---|---|---|---|
| simpleblocks / simplebuilding-core-config / simplehammers / simpletweaks: Configure opens a config screen (4) | a (Fenster) | Configure-Icon bei x=1252; Screenshots zeigen Mod Menu mit aktivem Icon, kein Fehler im client.log (ModMenu loggt Factory-Fehler) | ui_probe eigenes Xvfb + Fenster-Check |
| simplebuilding-core-config: wählte "Simple Building Tools" | a | Shot config-modmenu-selected: Suche "simple building" = 1/71 (Kern heißt "SimpleBuilding") | Suche "super mod" (nur in der Kern-Beschreibung) |
| simplemodels: Face tab; File; editor Back; browser Back (4) | a (Fenster) | Face x≈1195, File/Back in der Werkzeugleiste oben rechts; Back-Checks sind Folgefehler von File | wie oben |
| simplequestionary: right arrow 1->2, last page stays 2, left arrow 2->1 (3) | a (Fenster) | rechter Pfeil x≈1100; linker Pfeil/Mausrad/Tasten grün; 3. ist Folgefehler | wie oben. Nebenbefund (nicht geändert): JEI-Zutatenliste überdeckt den rechten Buchrand (kosmetisch) |
| simplebeacons: Regeneration unlocked at level 5 | a (Timing) | Luck (Reihe 4) schon bei Stufe 4 aktiv, Regeneration erst ab 5 (BeaconKind.secondaryLevel = Reihen+1); Leuchtfeuer zählt alle 80 Ticks, Platten 0,8 s auseinander -> Zwischenstand 4; zweiter Besuch Haste+Regen grün | auf Luck UND BeaconPowerButton aktiv warten |
| simpleblocks-scenes: frost grass crosshair | a | Shot: Umriss hinter der Schneeschicht; Strahl zur Zellmitte (y+0,5) fliegt über die 2-px-Schicht | auf den Frostgras-Block darunter zielen (Strahl tritt durch den Schnee ein) |
| simplecrucibles: reserved result in hidden ghost slot | a | CrucibleBlockEntity.reserve: Ergebnisplatz = Feld darunter (F13), updateGhosts setzt ghost[target], nie ghost[0] | alle Geisterfelder n..2n-1 prüfen |
| simpleend: real right click boards the boat | a | Standpunkt z+3,5 liegt im Wasserbecken -> Spieler sinkt, Strahl unter dem Boot (crosshair None) | Trittstein im Becken, Standpunkt z+4,5, Fadenkreuz vor dem Klick protokolliert |
| simplefarming: sweeping ring B (2) | c | Mod schneidet nur Zellen im 4,5-Block-Reichweitenschutz (InteractionGuard.MAX_REACH, Augenabstand zur Zellmitte); hintere Reihe z+5 ≥4,6 entfernt; geschnitten wurden exakt die 6 erreichbaren Ringzellen | Erwartung = Ringzellen in Reichweite (gerechnet), die anderen bleiben |
| simplefun: thrown brick breaks glass | a | beide Shots unter Wasser vor der Beckenwand: z+3,5 liegt im Becken von Szene 6 (z+3..12) | Standpunkt z+1,5 auf dem Gras |
| simplemobmanners: bucket keeps its variant (3) | a | Tooltip zeigt "Variant: Warm" (Komponente da), Gametest frog_bucket prüft sie; NBT-Pfad-Probe rot | `execute if items ...[minecraft:frog_variant=...]` ODER SelectedItem-Daten |
| simplemusic: right click removed the note | a | NoteGrid.toCode strippt -> ohne Note exakt "tempo 6" (NoteGridTest), Probe erwartete "tempo 6\n" | Erwartung "tempo 6"; Vorcheck auf "unless tempo 6" geschärft |
| simplemusic: sheet signed with the typed title | a | Beispielwahl in (d) setzt leeren Titel auf "First Steps", Upload speichert; Tippen hängt an -> "First Stepsprobe" | Titel vorher Strg+A/Rücktaste |
| simplemusic/pocket: right click plays / second click pauses (2) | a | Chat im Shot: "Expected literal ~ ...ic:playing]" – simplemusic:playing ist transient (kein Codec), der Item-Prädikat-Parser lehnt es ab, Probe-Befehl lief nie | persistenter Zustand simplemusic:pocket: Start = id gesetzt + ticks 0, Pause = ticks > 0 |
| simplepads: Shift + right click loads the rest | a (Timing) | Schleichtaste wird erst im Spieler-Tick übernommen, nach dem Klick desselben Frames; 0,3 s = ein Frame im Vollauf | 1,2 s Schleichen vor dem Klick |
| simplepads: double tap Space starts flying | a (Timing) | Sprungtaste wird je Tick gelesen; 60-ms-Tipper bei wenigen FPS verschluckt (y unverändert) | bis zu 4 Tipp-Takte (60..200 ms), erster Erfolg zählt |
| simplesandwiches: cheese 4 cuts | a | Schnitt von oben senkt den Block (SliceBlock-Form slices/16); Ziel y+0,99 liegt ab 14/16 über dem Block -> genau 2 Schnitte | vor jedem weiteren Klick auf die aktuelle Oberkante zielen |
| simplesandwiches: bundle eating | a | "hungry: False" (Essen 20, Rindfleisch nicht essbar) + Schleichen-Timing wie Pads | Goldäpfel (immer essbar), 1,2 s Schleichen vorher, Prüfung per `if items` |

## Nicht getestet
Kein Client-Lauf durch mich (Regel). Alle Fixes sind aus Code + Screenshots abgeleitet; Bestätigung braucht den Neulauf der Szenarien
(siehe Bericht). Annahmen mit Restrisiko: frog_variant-Prädikat (beide Wege eingebaut), Pocket-ticks-Feld (Default 0 evtl. weggelassen –
beide Fälle abgefragt), Doppeltipp abhängig von der Bildrate.
