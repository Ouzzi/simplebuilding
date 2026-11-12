# Audit simplefoundry (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases).
Umfang: 22 Java-Dateien / 1,8k Zeilen ohne Tests (mit Tests 25 / 2,5k). Klein genug fuer ein Vollaudit der Logik; Aufbau wie
`modules/simplestorage/AUDIT.md`.

## Vollstaendig gelesen
- Menue: `block/FoundryMenu.java` (Slots, Schnellverschieben, `clickMenuButton`, `stillValid`).
- BlockEntities: `block/DeskBlockEntity.java` (Tank, Brennstoff, Schmelzen, Giessen, Fenster-Daten, Laden/Speichern, Abbau),
  `block/TrayBlockEntity.java` (Giessform, Abkuehlen, Abbau).
- Bloecke: `block/DeskBlock.java`, `block/TrayBlock.java`, `ChamberBlock`, `SpoutBlock`, `GrateBlock`; Loot-Tabellen aller 6 Bloecke.
- Regeln: `FoundryScan.java` (Strukturpruefung), `Melts.java`, `Fuels.java`, `Metal.java`, `Tier.java`, `Layers.java`, `CastTarget.java`,
  `config/FoundryConfig.java` (Grenzen), `FoundryRegistry.java`, `SimpleFoundry.java`, Fabric-Einstieg.
- Nicht gelesen: `client/FoundryScreen.java` nur auf Pakete geprueft (nur `handleInventoryButtonClick` 0 und 100+n), Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads. Fensterknoepfe: 0 = Pfeil, 100..163 = Ebene; andere Ids `false`, Ebene wird beim Lesen auf die Struktur geklemmt. |
| C2 Server-Autoritaet | `stillValid`: Pult nicht entfernt, Abstand <= 8. Schmelzen, Giessen, Brennstoff nur serverseitig. |
| C3 Dupes/Verlust | Schnellverschieben ohne Endlosschleife, Plaetze nehmen nur Schmelzbares, Brennstoff-Slot nur Brennstoff; Lava-Eimer kommt leer zurueck. Einnahmen begrenzt durch Kapazitaet (Tank) bzw. 40000 Brennticks. Ausruestung schmilzt nach Abnutzung abgerundet (nie mehr als ihre Kosten). Loot-Tabellen nur der Block. **P2-1 gefunden und gefixt, P2-2 Rueckfrage.** |
| C4 NBT | Werte auf >= 0 geklemmt, Stufe/Brennhitze/Metall bereichsgeprueft, Giessform auf 9; Slots einzeln per `ItemStack.CODEC`. |
| C5 Tick-Kosten | Leeres Pult arbeitet nicht; aktive Struktur alle 100 Ticks geprueft (max. 512 Kammern, 32 je Achse, laedt keine Chunks). Kammer-Fenster nur geaenderte Zustaende, hoechstens alle 10 Ticks. P3-2 (Rechtsklick). |
| C6 Leaks | Keine statischen Sammlungen mit Spielern/Welten (`shown` je Pult). |
| C7 Sicherheit/Rechte | Keine Befehle ausser der Config (SimpleLib), keine Mixins. Weltaenderung nur an eigenen Kammern/Rosten. P3-3 (Schalter). |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel), keine Pflicht-Abhaengigkeit. |
| C9 Doku/Pflege | Keine TODO/FIXME, Lang EN=DE (45/45), keine Kern-Dubletten. Der alte Kommentar "Breaking the desk gives the fuel back" war falsch (es fielen nur die Slots) - mit P2-1 ersetzt. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplefoundry-P2-1 | P2 (Verlust) | Pult und Giessform gaben ihren Inhalt nur in `playerWillDestroy` heraus. Explosion (Creeper/TNT), Befehl oder das Flaechenwerkzeug eines anderen Mods entfernen Bloecke ohne diesen Weg; beide BEs sind kein `Container`, Vanilla wirft also nichts ab: bis zu 13 Slot-Stapel bzw. 9 Barren verschwanden. | **gefixt**: Abwurf in `preRemoveSideEffects` (DeskBlockEntity.java:446, TrayBlockEntity), `playerWillDestroy` entfernt (ein Weg). Gametest `removal_without_player_drops_slots_and_ingots` |
| simplefoundry-P2-2 | P2 (Verlust, Mechanik) | Die Pult-Stufe (Eisenblock + Diamantblock + Netheritbarren, DeskBlockEntity.java:121) haengt nur am Block-Entity; Abbauen/Versetzen gibt ein Stein-Pult zurueck, die Aufwertung ist weg. | **Rueckfrage** AUDSB31B-C1-1 (Nummer vom Hauptagenten einzuordnen): A) Stufe bleibt am Item (Komponente, Loot-Tabelle kopiert sie) - Empfehlung; B) Aufwertungs-Items fallen beim Abbau zurueck; C) bleibt so, Hinweis im Handbuch |
| simplefoundry-P3-1 | P3 | Tank (Schmelze, wartende Einheiten) und Brennstoff gehen beim Abbau verloren (bewusst, Kommentar DeskBlockEntity.java:431); per Rechtsklick eingeworfene Erze/Barren sind damit weg. | Hinweis (im Handbuch nennen) |
| simplefoundry-P3-2 | P3 (Server-Last) | Jeder Rechtsklick auf das Pult prueft die ganze Struktur neu (`revalidate`, Flutfuellung bis 512 Kammern; DeskBlock.java:95, DeskBlockEntity.java:251), ungedrosselt bei Paket-Spam. | Empfehlung: Ergebnis desselben Ticks wiederverwenden |
| simplefoundry-P3-3 | P3 | Config `enabled` und der Sub-Mod-Schalter blenden nur den Tab aus (SimpleFoundry.java:23); gebaute Giessereien arbeiten weiter. | Hinweis |
| simplefoundry-P3-4 | P3 (Balance) | Von Mobs fallengelassene Eisen-/Goldausruestung schmilzt zu vollen Barren (Vanilla-Ofen: 1 Nugget), Besitzer SA7 = A. | Hinweis |

## Urteil
**shipbar nach Fix** (0 P1, 1 P2 gefixt, 1 P2 Rueckfrage, 4 P3).

## Testergebnis
Lokal: `javac.sh simplefoundry` fuer `DeskBlockEntity`, `DeskBlock`, `TrayBlockEntity`, `TrayBlock`, `FoundryTests`, `ModuleGameTest` - nur bekannte
Access-Widener-Meldungen in nicht geaenderten Dateien. sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: echte Explosion (der Gametest entfernt per `setBlock`), Client, Forge/NeoForge.
