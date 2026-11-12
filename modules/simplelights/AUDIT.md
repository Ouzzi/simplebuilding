# Audit simplelights (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric. Aufbau wie `modules/simplestorage/AUDIT.md` und `modules/simplepads/AUDIT.md`.
Umfang des Mods: 11 Java-Dateien / 1,1k Zeilen (ohne Tests, ohne Forge/NeoForge; davon 447 Zeilen erzeugt: `LightsGuide`, `LightsDiscover`), Tests 722 Zeilen.
37 Lichtscheiben-Bloecke (sechs Seiten, 1 px dick, ohne Kollision), keine Block-Entities, keine Menues, keine Payloads, keine Mixins, keine Befehle,
keine Tick-Methoden (nur `neighborChanged`/`onPlace`).

## Vollstaendig gelesen
- Bloecke: `block/AbstractLightPaneBlock.java` (Platzierung, Halt, Wasser, Signal-Aktualisierung `refresh`), `LightPaneBlock.java`, `CopperLightPaneBlock.java`
  (Kupfer-Flip-Flop, Analogausgang), `RedstoneLightPaneBlock.java`, `CopperRedstoneLightPaneBlock.java` (Farbe je Signalstaerke, Rechtsklick), `LightPaneItem.java`.
- Rahmen: `SimpleLights.java` (IdMoves, Sub-Mod-Schalter), `LightsRegistry.java` (Eigenschaften, Licht je Zustand), `LightsFabric.java`, `fabric.mod.json`.
- Daten: `check_data.py` (37 Scheiben, Rezepte, Beute, Tags, Struktur).
- Gegengelesen in den Minecraft-Quellen 26.3: `Level#setBlock`/`markAndNotifyBlock` (Update-Flags), `ComparatorBlock#checkTickOnNeighbor`, `DiodeBlock`.
- Nicht gelesen: `guide/LightsGuide.java`, `discover/LightsDiscover.java` (erzeugt), Tests ausser dem Komparator-/Kupfer-Teil.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine Payloads. |
| C2 Server-Autoritaet | Zustand entsteht nur serverseitig (`refresh` bricht auf dem Client ab); Rechtsklick (Noten-Farbe) im `ServerLevel`. Kein Menue. |
| C3 Dupes/Verlust | Kein Container. Scheibe faellt ohne tragenden Block ab und gibt ihr Item (`updateShape` -> Luft, Vanilla `destroyBlock` mit Beute); `PushReaction.POPPED`; Beute je Scheibe 1 Stueck (`check_data` pruefte alle 37 Tabellen). Wasser: `SimpleWaterloggedBlock`, Flussigkeit bleibt (Tick geplant). |
| C4 NBT/Komponenten | Nur Block-Zustaende (`LIT`, `POWERED`, `START` 0-15, `COLOR` 0-16); alte Welten mit fehlenden Werten laden Standardwerte. Groesster Block: `copper_redstone_light_pane` mit 12 288 Zustaenden, `redstone_light_pane` 3 264 (Vanilla-ueblich fuer Mod-Bloecke). |
| C5 Tick-Kosten | Kein Tick. `neighborChanged` setzt nur bei geaendertem Zustand (`next != state`), ein Klick/Signal = ein `setBlock`; Lichtaenderung ueber `lightLevel` je Zustand. Klang/Partikel nur beim Rechtsklick/Kupfer-Flip, serverseitig, wie Notenblock/Kupferlampe. |
| C6 Leaks | Keine statischen Sammlungen mit Spielern/Entities/Leveln (nur Registrierungslisten). |
| C7 Sicherheit/Rechte | Kein Weltumbau ausser dem eigenen Zustand. Rechtsklick wie Notenblock (Vanilla-Interaktionsweg prueft Reichweite/Spawnschutz vorher). Keine Mixins, keine Befehle. |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel `runSimplelightsStandaloneGameTest`), nur `framework` + `simplelib`; SimpleBuilding nur optional (alte `simplebuilding:`-IdMoves). |
| C9 Doku/Pflege | Kein TODO/FIXME. Lang EN/DE gleich, amerikanisches Englisch geprueft. Keine Kern-Dubletten (`dup_pairs.txt`). **P2-1 gefunden und gefixt.** |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplelights-P2-1 | **P2 (Logikfehler)** | `AbstractLightPaneBlock#refresh` (AbstractLightPaneBlock.java:123-131) setzte den neuen Zustand mit `Block.UPDATE_CLIENTS` (Flag 2, wie die Redstone-Lampe). Die beiden Kupfer-Scheiben (`CopperLightPaneBlock`, `CopperRedstoneLightPaneBlock`) melden aber einen **Analogausgang** (15 solange sie leuchten, "wie die Kupferlampe", Kupferlampe nutzt Flag 3). Ohne Flag 1 meldet `Level#setBlock` weder Nachbarn noch `updateNeighbourForOutputSignal` (Level.java `markAndNotifyBlock`): ein Komparator, der die Scheibe liest, bekam den Wechsel nicht mit und blieb auf dem alten Wert, bis ihn etwas anderes anstiess. Die vorhandenen Tests pruefen den Ausgang nur direkt am Zustand, nie mit Komparator. | **gefixt** (Arbeitsbaum): Zustandswechsel mit `UPDATE_ALL`, wenn der neue Zustand einen Analogausgang hat, sonst unveraendert Flag 2 (`AbstractLightPaneBlock.java:130`); Gametest `copper_pane_tells_a_comparator_when_it_flips` (`LightsTests#copperPaneTellsAComparatorWhenItFlips`, Adapter `ModuleGameTest#copperPaneTellsAComparatorWhenItFlips`): Komparator hinter der Scheibe hat nach dem Signalanstieg einen geplanten Tick; ohne Fix rot |
| simplelights-P3-1 | P3 | `getStateForPlacement` spielt den Lampen-Klick (Kupfer-Flip bei Platzierung neben Strom) schon vor der endgueltigen Platzierung (AbstractLightPaneBlock.java:69-75 -> `CopperLightPaneBlock.java:44-46`); scheitert die Platzierung danach (Entity im Weg), bleibt ein Klick ohne Block. | Hinweis |
| simplelights-P3-2 | P3 | Rechtsklick auf die beiden Mehrfarbscheiben (`RedstoneLightPaneBlock.java:60-77`, `CopperRedstoneLightPaneBlock.java:82-102`) geht auch mit einem Block in der Hand vor dem Platzieren (Vanilla-Weg ueber `useWithoutItem`, wie Notenblock); Bauen an ihrer Flaeche nur im Schleichen. | Hinweis (bewusst) |

## Urteil
**shipbar nach Fix** (0 P1, 1 P2 gefixt, 2 P3). Kleiner, zustandsbasierter Mod ohne Netz, Menues, Block-Entities oder Mixins; Halt, Wasser, Beute und
Kupfer-Flip sind durch die vorhandenen Tests abgedeckt.

## Testergebnis
Lokal: `javac.sh simplelights` (AbstractLightPaneBlock, LightsTests, ModuleGameTest) - nur die bekannten Access-Widener-Meldungen in ungeaenderten Dateien
(`LightsRegistry.java:84`, `LibMaterials.java:100`); `modules/simplelights/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme, `tools/check_gametest_adapters.py` OK.
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: der neue Gametest wurde nicht ausgefuehrt (kein Gradle erlaubt); echte Komparator-Schaltung im Spiel, Piston/Beobachter-Kombinationen,
Forge/NeoForge (Modul ist Fabric-only).
