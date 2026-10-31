# Release-Audit QoL-A: Simple Quality of Life + Simple Interfaces

Branch `b-rel32-qola` (Basis b-rel32 d9cda1e21), 31.10.2026, Umfang MC 26.3 Fabric. Berichte: `modules/simplequalityoflife/AUDIT.md`, `modules/simpleinterfaces/AUDIT.md`. Prüfliste C1-C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Plan (Kopf)
Dateiliste und Reihenfolge: 1) simplequalityoflife (37 Dateien: 9 Java-Klassen + Fabric-Einstieg/Tests, Lang, Rezept/Advancement/Item, Questionary, FTB-Kapitel, `fabric.mod.json`, `check_data.py`), 2) simpleinterfaces (116 Dateien: 44 Mixins + `simpleinterfaces.mixins.json`, linked/ (5), tooltip/ (3), network/ (4), style/ (8), client/ (17), Konfiguration/Bildschirm, Tests, `fabric.mod.json`, Lang), 3) genutzte Kern-/simplelib-Teile (`SubMods`, `ConfigFiles`, `InteractionGuard`, `UiStyleToggle`, `UiBrightness`, `UiShiftTooltip`, `DevVariants`). Lesen ohne Gradle, jede Java-Datei der Mods; dann ein sbt-Lauf. Ausnahme: `client/keys`, `KeysScreen`, `KeysClient`, `KeyBindsScreenMixin`, `tools/check_client_mixins.py` laufen auf b-fix32 (nur gelesen).

## Ergebnis
| Mod | Urteil | P1 | P2 | P3 |
|---|---|---|---|---|
| simplequalityoflife | shipbar | 0 | 0 | 4 (1 behoben) |
| simpleinterfaces | shipbar | 0 | 1 (behoben) | 8 (3 behoben) |

Commits: INT-P2-1 (Easy Ender Chest aktualisiert Tooltip-Abbild), INT-P3-1 (Redirect → WrapOperation), INT-P3-2/3 + QOL-P3-1 (Config atomar/quarantäne, Texte).

## Abnahmeplan für den Besitzer

Voraussetzung: Fabric 26.3, Mod Menu + Cloth Config. Befehle brauchen OP.

### Simple Quality of Life (Super-Mod)
| ID | Aktion | Erwartetes Ergebnis |
|---|---|---|
| QOLA-QOL-1 | Spiel mit allen acht Jars starten, Mod Menu öffnen | Simple Quality of Life und die 7 Sub-Mods (Weather, Interfaces, Loot, Mob Manners, Farming, Tools, Movement) sind gelistet; Icon sichtbar |
| QOLA-QOL-2 | Mod Menu → Simple Quality of Life → Konfiguration | Ein Reiter "Sub-Mods" mit 7 Schaltern "Enable Simple XY", alle an; Tooltip jedes Schalters nennt "Default: true." (DE: "Standard: true.") |
| QOLA-QOL-3 | Im Bildschirm "Enable Simple Interfaces" aus, speichern, Welt öffnen, Truhe öffnen | Truhe sieht aus wie Vanilla; wieder an → Simple-Stil |
| QOLA-QOL-4 | `/simplequalityoflife submod weather false` | Meldung "Configuration saved: submod weather = false", `config/simplequalityoflife.json` zeigt `enableSimpleWeather: false`; `true` schaltet zurück |
| QOLA-QOL-5 | `/simplequalityoflife config` (Liste) | zeigt die 7 Schalter; Ändern wirkt wie QOLA-QOL-4 |
| QOLA-QOL-6 | Buch und Truhe formlos craften | "Simple Quality of Life Guide"; öffnen = Lesebuch mit 100 Seiten; Sprache auf Deutsch → Seiten deutsch |
| QOLA-QOL-7 | Kreativ-Inventar → Werkzeuge & Nützliches | Handbuch steht vor dem Buch und Feder |
| QOLA-QOL-8 | (optional, FTB Quests installiert) neue Welt | Startkapitel "Simple Quality of Life" vorhanden, erste Aufgabe gibt das Handbuch |
| QOLA-QOL-9 | Alte `simplequalityoflife.json` mit `"enableSimpleCreatures": false` ablegen, Spiel starten | Schlüssel wird zu `enableSimpleMobManners` umbenannt, Wert bleibt aus; Log "Renamed old sub-mod switches" |
| QOLA-QOL-10 | Dedicated Server nur mit den 8 Jars starten, Client mit denselben verbinden | Server startet ohne Fehler, Beitritt klappt, Schalter-Befehl wirkt |

### Simple Interfaces
| ID | Aktion | Erwartetes Ergebnis |
|---|---|---|
| QOLA-INT-1 | Truhe, Fass, Endertruhe, Trichter, Werfer öffnen | Farbiger Kasten je Block (Eiche, Fass, Ender-Türkis, Eisen, Stein), heller Inventar-Kasten, Titel links oben im Kasten, keine Vanilla-Textur |
| QOLA-INT-2 | Gefärbte Shulkerkiste öffnen | Kasten in der Farbe der Kiste (ungefärbt violett) |
| QOLA-INT-3 | Werkbank, Ofen mit Brennstoff, Brauständer, Zaubertisch, Leuchtfeuer öffnen | Simple-Stil je Block; Ofen: Flammen im Brennstoff-Slot, Pfeil füllt sich; Zaubertisch: Angebote als Reihen mit Lapis-Edelsteinen |
| QOLA-INT-4 | Amboss (Reparatur mit zu hohen Kosten), Schmiedetisch, Schleifstein, Steinsäge, Webstuhl, Kartentisch, Dorfbewohner-Handel, eigenes Inventar (E) | Simple-Stil; Fehler als rotes Kreuz; Amboss zeigt XP-Kosten im Kasten; gewähltes Angebot eingesenkt |
| QOLA-INT-5 | Konfig → Haupt-Schalter "Simple style" aus (und Test: einzelner Bildschirm-Schalter) | Alle (bzw. nur dieser) Bildschirme wieder wie Vanilla |
| QOLA-INT-6 | Mit leerer Haupthand Schleichen + Rechtsklick auf eine Truhe | Klang, violette Partikel an der Hand, Banner mit Truhen-Icon rechts am Bildschirmrand |
| QOLA-INT-7 | Danach Ofen öffnen | Über dem Ofen ein zweiter Kasten "Verknüpft: ..." mit den Truhen-Slots; Shift-Klick bewegt Items zwischen Truhe, Ofen und Inventar ohne Verlust; Rad scrollt bei vielen Reihen |
| QOLA-INT-8 | Mit aktiver Marke Esc drücken | Banner verschwindet, Pausenmenü öffnet NICHT; zweites Esc öffnet es |
| QOLA-INT-9 | Truhe markieren, entfernen (>64 Blöcke, oder `/simpleinterfaces config set linkedContainerRange 8`), oder abbauen | Marke endet, Panel verschwindet, Menüs bleiben konsistent |
| QOLA-INT-10 | Eigene Truhe markieren, selbst öffnen und schließen | Marke endet nach dem Besuch |
| QOLA-INT-11 | Shulkerkiste in die Hand, in die Luft rechtsklicken; danach per Rechtsklick auf die Kiste im Inventar | Kiste öffnet ohne Platzieren; Slot der Kiste ist gesperrt (kein Verschieben, keine Zahlentaste, Q); Items bleiben nach Schließen in der Kiste |
| QOLA-INT-12 | Endertruhe-Item in die Hand, rechtsklicken, Item in die Endertruhe legen, schließen, mit der Maus über das Endertruhe-Item fahren, Shift halten | Tooltip zeigt die gerade gelegten Items (Befund INT-P2-1) |
| QOLA-INT-13 | Befüllte Shulkerkiste im Inventar, Maus darüber, Shift halten; ohne Shift | Mit Shift Slot-Raster in Kistenfarbe, ohne Shift Hinweis "Umschalt halten für Inhalt" |
| QOLA-INT-14 | Bündel mit >12 Einträgen: Tooltip, Mausrad | Alle Einträge sichtbar (bis zu 6 Reihen, dann Seiten mit "+n"); Mausrad wählt jeden Eintrag |
| QOLA-INT-15 | Fernglas benutzen; Mausrad | Dünner Fensterrahmen am Rand statt schwarzem Kreis, HUD weg (Fadenkreuz bleibt), stufenloser Zoom, Maus wird träger |
| QOLA-INT-16 | Konfig `zoomHotkey` an, C halten ohne Fernglas | Gleiche Ansicht, Mausrad zoomt; C loslassen beendet |
| QOLA-INT-17 | Hovern über Inventar-Slot mit Item | Hervorhebung nur hinter dem Item; Beinschutz von hinten/unten ohne Loch |
| QOLA-INT-18 | Mit Vanilla-Client auf einen Server mit der Mod | Vanilla-Menüs ohne Fehler; Easy Shulker funktioniert (serverseitig); Marke zeigt kein Panel |
| QOLA-INT-19 | `/simplequalityoflife submod interfaces false` mit Marke und offenem Easy-Shulker | Marke endet, Kisten öffnen sich nicht mehr in der Luft, Screens Vanilla; `true` stellt alles her |
| QOLA-INT-20 | (nach b-fix32) Steuerung → Button "Tasten" bzw. Taste "Tastenbelegung öffnen" | Neue Tastenseite mit Liste, Suche, Konflikte, Tastatur; Eintrag ändern wirkt sofort |

## Release-Punkte

### simplequalityoflife
- **Version/Changelog (Entwurf):** 1.0.0. "Erste Veröffentlichung als Paket: Schalter je Sub-Mod (Weather, Interfaces, Loot, Mob Manners, Farming, Tools, Movement), Befehl `/simplequalityoflife`, Handbuch mit 100 Seiten (Buch + Truhe), FTB-Quests-Startkapitel. Alte Einstellungen (`qOL.*`) werden von den Sub-Mods übernommen, `enableSimpleCreatures` wird zu `enableSimpleMobManners`."
- **Kurzbeschreibung (Entwurf):** Small comfort features for Minecraft 26.3 in seven sub-mods with one switch each.
- **Langbeschreibung (Entwurf):** Simple Quality of Life bundles Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools and Simple Movement. Each sub-mod also runs on its own. Switch each one off in Mod Menu or with `/simplequalityoflife submod <name> <true|false>`. Adds a guide book (craft a book and a chest together). Requires Fabric API and Cloth Config; Mod Menu optional.
- **fabric.mod.json:** `depends` auf alle 7 Sub-Mods + fabric-api + cloth-config (korrekt, alles verschachtelt). Befund: `icon` zeigt auf `icon.png` (16 px) → `assets/simplequalityoflife/icon_128.png` für Plattformen (Entscheidung Hauptsitzung, QOL-P3-3). Lizenz CC0-1.0, Kontakt gesetzt. `version` kommt aus `moduleVersion` in `fabric/build.gradle` (0.9.0 → auf 1.0.0 setzen).
- **Sonst:** `DevVariants.DEFAULT_ON` (Hauptsitzung), Wiki-/Handbuchseiten aktuell (`check_data.py` grün).

### simpleinterfaces
- **Version/Changelog (Entwurf):** 1.0.0 (aktuell 0.1.0, `moduleVersion` fehlt in `fabric/build.gradle`, Standard 0.1.0 → eintragen). "Simple style for 22 Vanilla container screens, linked containers, Easy Shulker/Ender Chest, container and bundle tooltips, spyglass/zoom overlay, key binding page."
- **Kurzbeschreibung (Entwurf):** Vanilla container screens in a coloured Simple style, linked containers, shulker and ender chest previews.
- **Langbeschreibung (Entwurf):** Simple Interfaces restyles chests, shulker boxes, workstations, villager trading and your inventory in one consistent style (box colour follows the block). Sneak + right-click a container to link it: the next GUI shows it above, with shared inventory. Open shulker boxes and ender chests straight from your hand or inventory. Shulker and ender chest tooltips show a slot grid with Shift, bundles show every entry, the spyglass gets a slim window frame with stepless zoom. Everything has a switch; server-side parts (linked containers, Easy Shulkers) are configurable per server. Requires Fabric API and Cloth Config; Mod Menu optional.
- **fabric.mod.json:** `depends` korrekt (`simplelib >=0.3.0` verschachtelt), Icon 16 px (siehe oben), Mixins-JSON `required: true`, `environment: *` richtig (Server-Mixins + Client-Mixins). Keine Plattform-Probleme erkannt.
- **Hinweis:** `StyleToggleClient.development` zeigt den Vergleichsknopf nur in der Entwicklungsumgebung oder wenn `showStyleToggle` an ist (Standard aus): im Release unsichtbar.

## Rückfragen
- R-INT-1: Zoom-Taste C soll auch bei ausgeschalteter Funktion belegt bleiben (kann Konflikte mit anderen Zoom-Mods anzeigen) oder standardmäßig unbelegt? Standardvorschlag: unbelegt lassen, wenn `zoomHotkey` aus ist (Besitzer bestimmt).
