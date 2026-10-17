# Prinzipien der SimpleBuilding-Familie (Einstieg, Stand 17.10.2026)

Eine Stelle für alle Regeln: **Teil 1** gilt für jede Mod der Familie (SimpleBuilding und alle Module unter `modules/`), **Teil 2** gibt je Mod
(Sub-Mod) die eigenen Regeln. Dies ist der Einstieg; die ausführlichen Texte bleiben in den verlinkten Dateien und in `AGENTS.md`
(maßgebliche Regelsammlung für Assistenten). Neue Regeln des Besitzers hier eintragen (Teil 1 oder beim passenden Modul) und von dort verlinken.
Regel-IDs: A1–A10 gelten für alle Mods; die Besitzer-Regeln vom 17.10. tragen zusätzlich die IDs B1 (Baublöcke, = A3) und O1 (Octets, = A4).

## Teil 1 – Für alle Mods

### A1 Jedes Modul allein spielbar
Alle Module starten und sind mit Vanilla + harten Bibliotheken komplett spielbar; Partner-Mods machen es besser, nie erst möglich. Harte Abhängigkeit nur als erklärtes
Add-on, keine Klassenimporte über Modulgrenzen (nur Framework-API, öffentliche Registry-Ids, Tags). Registrierung hängt nie vom Laden eines anderen Mods ab
(nur Bezug/Sichtbarkeit/Rezepte). Ausnahme bei Fremdblock-Varianten: siehe O1.
→ `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, `docs/MULTIMOD.md`, `docs/ai/PLAN-MODUL-UNABHAENGIGKEIT-2026-10-02.md`; AGENTS.md §10.

### A2 Modulfamilie und simplelib (AGENTS.md 3.16)
SimpleBuilding ist Super-Mod mit Sub-Mods (Octets, Blocks, End, Ovens, Crucibles, Mobheads, später Hammers, Storage, Gadgets, Enderite). Was mehrere Module brauchen
(z. B. polierter Endstein, geteilte Materialien, UI-Bausteine, In-World-Upgrades), liegt in `simplelib`. Ist ein Partner-Modul geladen, schalten sich passende Varianten frei.
Neue Features werden nach dem Mods-Verzeichnis eingeordnet (`docs/MODS-UND-FEATURES.md`, im selben Commit pflegen).
→ `docs/ai/PLAN-MODULFAMILIE-2026-10-16.md`, `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md`, `docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`.

### A3 Baublöcke gehören in die Familie, Rest in Simple Blocks (B1, AGENTS.md 3.16, verfeinert 17.10.)
Alle Baublöcke gehören zur SimpleBuilding-Familie, nie in fachfremde Mods. **Simple Blocks enthält alle Baublöcke, die keinen anderen fachlichen Ort haben**
(Themen-Sub-Mods wie Octets, End/Enderite, Schach behalten ihre Blöcke; Lager/Maschinen/Werkzeuge bleiben, wo sie sind). Verschiebe-Liste:
`docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`. Es gibt **einen** Kreativtab „Simple Blocks“ für alle Baublöcke (Plan dort).

### A4 Octet-Regel (O1, 17.10.)
Jeder Block, der Treppen- oder Stufen-Varianten (stairs/slab) hat, bekommt auch einen Octet-Block (Achtelblock) – Vanilla-Blöcke, SimpleBuilding-Blöcke und alle Blöcke,
denen Simple Blocks Treppe/Stufe gibt. Das Octet liefert `simpleoctets` (bei Fremdblöcken als optionale Integration, nur wenn der Besitzer-Mod geladen ist; jeder Mod bleibt allein spielbar).
**Ausnahme (Besitzer 17.10.):** Naturstufen (Erde, Gras, Sand, Kies) bekommen keine Octets. Neue Treppen/Stufen in irgendeinem Modul ohne Octet-Eintrag sind ein Fehler (Abdeckungstest). Lücken: `docs/ai/INVENTAR-OCTETS-2026-10-17.md`.

### A4b Formsäge (Besitzer 17.10.)
Alles, was Treppen-, Stufen- oder Octet-Varianten hat (Stein, Holz und Blöcke, die die Vanilla-Steinsäge nicht kann), lässt sich in der fortgeschrittenen Säge schneiden (Arbeitsname „Formsäge“, Simple Blocks). Neue Formen müssen dort auftauchen.

### A5 Vanilla-Rückports
Künftige Vanilla-Features je Version prüfen: hat Vanilla es schon, ist der Mod-Teil aus; bei jedem Port erneut prüfen; Liste und Flags in der Datei.
→ `docs/ai/PRINZIPIEN-VANILLA-RUECKPORTS.md`

### A6 Entdeckbarkeit und Filter
Jeder Weg ist zuerst intuitiv und zusätzlich im Spiel gehintet (mindestens zwei Hinweise, einer im Spiel-Alltag; Wiki zählt nicht allein). Filternde Blöcke lassen ein echtes Item fest im Slot
und teilen den Filter-Knopf (exakt / gleiche Art, gemeinsamer Code in simplelib).
→ `docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md`, `docs/ai/PRINZIPIEN-FILTER.md`

### A7 Lesbarkeit und UI
Jede UI-Änderung: Kontrast zum Hintergrund ≥ 4,5:1 (nie dunkler Text mit dunklem Schatten auf Grau), Kontrastwerte im Bericht, Test `ui_text_contrast` bleibt grün (AGENTS.md 3.15).
Tooltips: Standard nur der Name, Beschreibung nur bei nicht offensichtlicher Bedienung oder unsichtbarem Zustand, höchstens 2 Zeilen à ca. 40 Zeichen; Balken-Information nicht doppeln (3.14).
Kein Text auf dem Bildschirm für Gadgets (3.4). Amerikanisches Englisch, EN+DE parallel (3.5).

### A8 Texturen
Neue Pixelkunst im Stil des Besitzers, immer mit 16-fach-Vorschau alt neben neu; alte Besitzer-Texturen respektieren (3.7). **Aus Vanilla abgeleitete Pixel werden nie eingecheckt**
(auch keine Wiki-Kopien oder Vorschau-PNGs), sondern beim Build erzeugt: `tools/textures/vanilla_derived.py`, Pfade in `.gitignore`.

### A9 Server-Autorität und Belege
Alle Stellschrauben serverseitig mit Obergrenzen, die Vanilla nicht gefährden (3.8). Wiki-Aussagen müssen im Code beweisbar sein (3.6); Handbücher/Wiki nach Änderungen neu erzeugen.

### A10 Arbeitsweise
Queue-Punkte vor Arbeitsbeginn mit `[~] (A|B, <branch>, <Datum>)` beanspruchen (`docs/ai/ZWEI-SESSIONS.md`, 3.17); Push nur nach grünem Gate (3.2); Hauptlinie 26.3 zuerst, Fabric zuerst (3.1);
Ports (26.2, 1.21.11, 26.4) erst auf Anforderung. Abläufe: `docs/ai/WORKFLOW.md`, Tests/Gate: AGENTS.md §4.

---

## Teil 2 – Je Mod

Reihenfolge wie `modules/modules.json`; Mods ohne Sonderregeln stehen mit „keine eigenen Regeln bisher“ da, damit jede Mod einen Platz für ihre Regeln hat. Neue Module bekommen hier
einen Abschnitt (`python tools/newmod.py` legt den Manifest-Eintrag an; den Abschnitt von Hand ergänzen).

### SimpleBuilding (`simplebuilding`)
Super-Mod der Familie: Hülle mit Konfig, Mega-Guide, Testzentrale, Baugeräte (Stäbe, Meißel, Blaupause, Oktant), Hämmer, Lager, Maschinen, Eimer, Schach, Pads.

- Keine neuen Baublöcke ohne Eintrag in `docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`: Baublöcke ohne Themenmodul gehören nach `simpleblocks` (B1), Achtel nach `simpleoctets` (O1).
- Tooltip-Regel (AGENTS.md 3.14): Standard nur der Name; Eimer zeichnen nie eine Haltbarkeitslinie.
- Gadgets zeigen keinen Text auf dem Bildschirm (AGENTS.md 3.4): Sounds, Partikel, Blockzustände.
- Pflicht-Sub-Mods sind gebündelt (`requires` in `modules.json`); alles andere nur als `optional` (Schalter `enableSimpleX`).
- Schach-Achtel (`checker_octet`) und Schach-Figuren bleiben hier (Plan F3).

### Wiring Example (`wiringexample`)
Minimales Beispiel-Modul für Lader-/Integrations-Verdrahtung.

- keine eigenen Regeln bisher

### Simple Money (`simplemoney`)
Währung, gewichtete Dorfbewohner-Handel, Schatz-Loot.

- Preise nehmen fremde Items nur über `prices.json` je Modul (`modules/<id>/…/money/<ns>/prices.json`), nie über Klassen.

### Simple Riding (`simpleriding`)
Sattel-Tempo, Pferdesprünge, Nautilus-Dashes, Reittier-Rüstungen.

- keine eigenen Regeln bisher

### Simple Models (`simplemodels`)
Server-genehmigte Item-Modelle, Amboss-Zuweisung, Modellbrowser.

- keine eigenen Regeln bisher

### Simple Fun (`simplefun`)
Spielerische Mechaniken (Wurfziegel, kosmetische Verwandlungen).

- keine eigenen Regeln bisher

### Simple Visuals (`simplevisuals`)
Client-HUD, Tooltips, Amboss-Formatierung, Immersions-Partikel.

- UI-Änderungen: Kontrast ≥ 4,5:1 (Regel A7).

### Simple Sounds (`simplesounds`)
Lokale Vanilla-Klangpartner der zwölf Immersions-Effekte.

- keine eigenen Regeln bisher

### Simple Quality of Life (`simplequalityoflife`)
Super-Mod für die QoL-Sub-Mods (Wetter, Interfaces, Loot, Mob-Manieren, Farming, Tools, Movement), je ein Schalter.

- Jedes Sub-Mod ist allein spielbar und hat seinen eigenen `modules.json`-Eintrag; der Super-Mod bündelt sie nur (`requires`).

### Simple Tweaks (`simpletweaks`)
- Standardwerte (Besitzer 17.10.): alles, was nur per Befehl ausgelöst wird, ist standardmäßig an; automatische Verhaltensänderungen nur, wenn der Besitzer es so festgelegt hat.
Legacy-Kompatibilität, serverautoritäre Claims (Standard an).

- Server-Autorität (AGENTS.md 3.8): alle Stellschrauben serverseitig mit Obergrenzen.
- Befehle (Besitzer 17.10.): Alle Befehle auch unter `/simpletweaks <befehl>` (ein Wort, ohne Bindestrich; Redirect auf dieselben Knoten, gleiche Rechte). Neue Befehle per `TweakCommands.add(dispatcher, literal(...))` registrieren (oder `TweakCommands.alias(...)` nach der Registrierung; `/tweaks`-Zweige werden automatisch übernommen) und den Register-Aufruf der Gruppe im Gametest (`AdminTests`) ergänzen; `/simpletweaks help` listet nur erlaubte; Doku und Wiki nennen die `/simpletweaks`-Form.

### Simple Dimensions (`simpledimensions`)
Konfigurierbare Dimensionen, Lichtbögen, sichere verknüpfte Portale.

- keine eigenen Regeln bisher

### Simple Sandwiches (`simplesandwiches`)
Sandwiches auf dem Schneidebrett, Küchenmesser, Käse und Butter.

- Gemeinsame Nahrung („warme Speisen“) liegt in simplelib.

### SimpleLib (`simplelib`)
Gemeinsame Bibliothek: geteilte Materialien, IdMoves, In-World-Upgrades, UI-Bausteine.

- Alles, was mehrere Module brauchen, liegt hier (Regel A2), mit Mindestversion in den Abhängigkeiten.
- UI-Bausteine (z. B. `UiFilterButton`, Filter-Prinzip A6) hier dokumentieren und wiederverwenden, nicht kopieren.

### Simple Interfaces (`simpleinterfaces`)
Container-Bildschirme im Kastenstil, verknüpfte Container, Easy Shulkers / Ender Chests; Sub-Mod von QoL.

- UI-Kontrast ≥ 4,5:1 (A7); Stil nah an Vanilla.

### Simple Maps (`simplemaps`)
Wegweiser-Karten, Wegpunkte, gerahmte Ansichten, Kartentisch-Upgrades.

- keine eigenen Regeln bisher

### Simple Trims (`simpletrims`)
Leuchtende/Emittierende/Pulsierende Besatzvorlagen, platzierbare Vorlagen, Besatz-Upgrades.

- In-World-Upgrades: Axt allein, Vorschlaghammer mit SimpleBuilding.

### Simple Mobs (`simplemobs`)
Hollow Howler, Hollow Watcher, Shellker, Soulfire Blaze, Dread, Deceiver, Blighted Wither, Potion of Silence, Biome Brush.

- Sculk-Kiefer (Howler-Falle) gehört thematisch hierher; liegt bis zur Verschiebung in SimpleBuilding.

### Simple Weather (`simpleweather`)
Wetter-Schalter (Server), Regendichte (Client); Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Movement (`simplemovement`)
Manuelles Kriechen, Autowalk, schnellere Leitern, Frost Walker auf Pulverschnee; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Tools (`simpletools`)
Volle Haltbarkeit, Thrift und Soulbound, Todes-Tresor, Amboss-Reparaturen; Sub-Mod von QoL (kein Werkzeug-Item).

- Namenskollision beachten: Werkzeug-Items (Hämmer, Meißel, Stäbe) gehören nicht hierher, sondern zu SimpleBuilding bzw. dem geplanten `simplehammers`.

### Simple Farming (`simplefarming`)
Federfall schützt Ackerland, Schärfe schneidet Gras, Hacke erntet und pflanzt neu; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Mob Manners (`simplemobmanners`)
Mobs stummschalten, benannte Babys jung halten, Piglins ignorieren Gold; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Loot (`simpleloot`)
Tresor-Abklingzeit in Tagen; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Questionary (`simplequestionary`)
Fragentisch: Item einlegen, Lore und Geheimnis im offenen Buch.

- keine eigenen Regeln bisher

### Simple Aesthetics (`simpleaesthetics`)
Super-Mod ohne eigenen Inhalt: Visuals, Sounds, Models mit je einem Schalter.

- keine eigenen Regeln bisher

### Simple End Dragon Fight (`simpleenddragonfight`)
Enderdrachenkampf: Lebensbalken je Spieler, schnellerer Drache, Dragonmites.

- keine eigenen Regeln bisher

### Simple End (`simpleend`)
Endholz (Aschholz, Dämmerholz), Knospen, Bäume, Endgras, Biome auf den äußeren Inseln; bündelt Simple End Dragon Fight.

- Hölzer mit Treppe/Stufe bekommen Bretter-Achtel in Simple Octets (O1), bedingt bei geladenem Simple End.
- Künftig: End-Baublöcke (polierter Endstein-Familie) – gemeinsamer Stein liegt in simplelib.

### Simple Respawn (`simplerespawn`)
Niedergeschlagen statt tot: Teamkameraden können wiederbeleben.

- keine eigenen Regeln bisher

### Simple Beacons (`simplebeacons`)
Starker Leuchtturm, starke Conduit, Upgrade-Vorlage.

- In-World-Upgrades: Axt allein, Vorschlaghammer mit einem Hammer-Mod.

### Simple Ovens (`simpleovens`)
Verstärkte, Netherit- und Enderit-Öfen, Räucher- und Hochöfen, Ofen-Loren; Pflicht-Sub-Mod von SimpleBuilding.

- In-World-Upgrades: Axt allein, Vorschlaghammer in SimpleBuilding. Ids wanderten aus SimpleBuilding (IdMoves).

### Simple Mobheads (`simplemobheads`)
Kopf für jeden Mob; Pflicht-Sub-Mod von SimpleBuilding.

- keine eigenen Regeln bisher

### Simple Shops (`simpleshops`)
Spieler-Shops: Verkaufsblock mit Preis, Vitrine mit Waren.

- Käufer brauchen kein Claim-Recht; Schutz vor Fremden, Explosionen, Kolben, Trichtern.

### Simple Wanderers (`simplewanderers`)
Dorf-Wanderer: ein fahrender Händler je Dorf mit Pferd, Warenwechsel alle 3 Tage.

- keine eigenen Regeln bisher

### Simple Stats (`simplestats`)
Statistiken je Werkzeug/Waffe/Rüstung mit eigenem Statistik-Bildschirm.

- keine eigenen Regeln bisher

### Simple Blocks (`simpleblocks`)
Fehlende Baublöcke: Hushed Deepslate Tiles, Sculk-Tiefenschiefer, fehlende Vanilla-Treppen/-Stufen/-Mauern, rissige/bemooste Ziegel; allein mit Vanilla spielbar.

- **B1 (17.10.):** Simple Blocks enthält alle Baublöcke der Familie, die keinen anderen fachlichen Ort haben (Deko-/Bauvarianten ohne Themen-Sub-Mod). Lager, Maschinen und Werkzeuge bleiben, wo sie sind. Bestand und Verschiebe-Liste: `docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`.
- Gibt Simple Blocks einem Block Treppe oder Stufe, gilt Regel O1: der Block bekommt ein Achtel (Eintrag im Octet-Generator von `simpleoctets`; Simple Blocks kennt Simple Octets nicht).
- Genau **ein** Kreativtab „Simple Blocks“ für alle Baublöcke der Familie (Plan im Baublock-Inventar, Abschnitt „Plan: ein Kreativtab“); Standalone-SimpleBuilding behält seinen eigenen Tab.
- Nur öffentliche Registry-Ids und Tags nach außen; verschobene Blöcke mit IdMoves/Alias, Registrierung nie vom Laden anderer Mods abhängig.

### Simple Octets (`simpleoctets`)
Achtelblöcke (kleine 1/8-Blöcke), gemischte Zellen, Steinsäge-Rezepte (Stufe 6, Branch `claude-s6-octets`, noch nicht in `claude-wave1`).

- **O1 (17.10.):** Jeder Block, der Treppen- oder Stufen-Varianten (stairs/slab) hat, bekommt auch einen Octet-Block (Achtelblock). Gilt für Vanilla-Blöcke, SimpleBuilding-Blöcke und alle Blöcke, denen Simple Blocks Treppe/Stufe gibt. Fremdblöcke nur bedingt bei geladenem Besitzer-Mod (`SimpleOctets.isLoaded`).
- Quelle der Wahrheit ist die Generator-Liste (`tools/textures/octets_2026_10_09.py`, `octet_materials.json`, `OctetMaterials`); der Abdeckungstest soll fehlende Familien automatisch melden. Stand und Lücken: `docs/ai/INVENTAR-OCTETS-2026-10-17.md` (23 fehlende).
- Bretter gehören dazu (frühere Ausnahme „ohne Bretter“ entfällt). Die Regel gilt einseitig: Octets ohne Treppe-Familie (Obsidian, Metallblöcke, Stämme, Terrakotta) bleiben.
- Schach-Achtel bleiben in SimpleBuilding (F3); die Zellen-Basisklasse gehört dem Modul.

### Simple Crucibles (`simplecrucibles`)
Eisen-, verstärkte, Netherit- und Enderit-Tiegel und -Fässer, verstärkter Kessel, Dorfküchen; Pflicht-Sub-Mod von SimpleBuilding.

- In-World-Upgrades: Axt allein, Vorschlaghammer in SimpleBuilding. Ids wanderten aus SimpleLib/SimpleBuilding (IdMoves).

### Geplante Module (noch nicht in `modules.json`)

- `simplehammers`: Geplant (Stufe 7): Hämmer, Hammer-Verzauberungen, Schnitzen, In-World-Crafting. Eigene Regeln: keine bisher.
- `simplestorage`: Geplant (Stufe 8): Lager (Kisten-Stufen, Rucksäcke, Fässer). Eigene Regeln: keine bisher.
- `simplegadgets`: Geplant: Geräte in der Hand (Rotator/Transposer, Messuhr, Detektor, Magnet, Oktant, Attractor). Eigene Regeln: keine bisher.
- `simpleenderite`: Geplant: Enderit-Ausrüstung, Astral/Nihil-Paletten. Eigene Regeln: keine bisher.
