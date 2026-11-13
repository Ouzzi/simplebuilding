# Audit simplebuildingtools (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric. Aufbau wie `modules/simplestorage/AUDIT.md` (Gruppe A).
Umfang: 88 Java-Dateien / 15,3k Zeilen ohne Tests (dazu 4 Testklassen / 6,4k Zeilen), Forge/NeoForge nicht Teil des Releases.
Risikogeleitet geprueft (P1-Klassen zuerst): alle C2S-Payloads, alle Werkzeug-Pfade, die Bloecke setzen oder raeumen, Kartentisch,
Mixins. Fruehere Audits sind im Code vermerkt (`docs/AUDIT-2026-09-26.md` #2, #6, #8, #9, #10, #11, #28, #29, #31; Nach-Audit N2,
N3, N8, N9, N13, N15, N16; SBG-03/04) und wurden nicht wiederholt.

## Vollstaendig gelesen
- Netzwerk: `networking/` (5 C2S: `OctantConfigurePayload`, `OctantScrollPayload`, `BuildingWandConfigurePayload`,
  `BlueprintEditPayload`, `BlueprintRotatePayload`; 1 S2C `BtConfigPayload`), `ModMessageHandlers.java`, Registrierung
  `BuildingToolsFabric.java`, Client-Sender `mixin/client/MouseMixin.java`, `platform/ClientNetworking.java`.
- Bau-Werkzeuge: `items/custom/BuildingWandItem.java` (Flaeche, Abdeckung, Linie, Bruecke, Materialsuche Inventar/Buendel/Rucksack,
  Verbrauch, Haltbarkeit), `util/WandPlacement.java`, `util/BuildPermissions.java`, `util/WandUndo.java`, `util/WandHunger.java`,
  `blueprint/BlueprintBuilder.java` (Planner, Zwei-Klick-Pruefung, Auftraege, Fortsetzen), `blueprint/BlueprintMaterials.java`
  (Kosten, `survivalState`), `blueprint/ShapeFill.java` (Oktant-Fuellung, Dach), `blueprint/BlueprintJobs.java`,
  `items/custom/CreativeReach.java`, `items/custom/OctantItem.java`, `util/OctantShape.java`, `util/OctantHammerArea.java`,
  `util/WornStorage.java`, `util/StorageRefs.java`, `util/OctantCauldronWash.java`.
- Kartentisch/Menues: `mixin/CartographyTableMenuMixin.java`, `mixin/AbstractContainerMenuMixin.java`,
  `blueprint/BlueprintCartography.java` (Slots, Ergebnis-Slot, Kopie), `blueprint/BlueprintScanner.java`.
- Rest: `items/custom/BlueprintItem.java`, `blueprint/BlueprintContent.java`, `component/BtComponents.java`, `config/BtConfig.java`,
  `config/BtTuning.java`, `blueprint/BlueprintPicture#refusal`, `SimpleBuildingTools.java`, `mixin/CauldronDispatcherAccessor.java`;
  Stichproben `BlueprintCode` (Grenzen, Cache).
- Nicht gelesen: Screens/Renderer/HUD unter `client/` (nur geprueft, dass sie keine Logik entscheiden), Parser `BlueprintCode` im
  Detail, Import/Export (`BlueprintFormats`, `BlueprintCommandText`, nur Client), Guide/Discover, die Gegenseite der abgelegten
  Blaupause (`PlacedAccess`, SimpleBuildings `editablePlacedBlueprint`), Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | 5 C2S, alle ueber `context.server().execute` auf dem Server-Thread. Oktant: Ecken nur in 320 Bloecken um den Spieler und in Bauhoehe, Form/Reihenfolge nur Enum-Namen (Codec 32 Zeichen), Ausrichtung 0-5, Scroll-Betrag auf +-16 begrenzt, gesperrter Oktant bewegt sich nicht. Baustab: Radius 0..Stufe, Achse 0..3. Blaupause: Slot nur Hotbar/Nebenhand, Code <= 32000, Titel 1-32, Signieren und `pixels`-Code zahlen aus einem Zeichenbudget je Spieler; Item bestimmt der Server. Rotation nur mit Stab + Blaupause in der Hand. S2C-Config <= 64 KiB, Client validiert. Ungedrosselt: unsigniertes Zwischenspeichern (wie Vanillas Buch). |
| C2 Server-Autoritaet / Reichweite | Klick-Reichweite prueft Vanilla (`useOn`); Kreativ-Reichweite (1024) wirft der Server selbst. Bau, Material, Rueckgaengig, Scan entscheidet nur der Server. Kartentisch: `stillValid` von Vanilla. Drei Client-Klassen ausserhalb `client/` (P3-9). |
| C3 Dupes / Item-Verlust | Verbrauch je gesetztem Block erst nach erfolgreichem Setzen (Erstattung bei Fehlschlag), Blaupause nur mit Items ohne Komponenten (#10), Ueberlebens-Zustaende ohne Wachstum/Fuellung (#2), Ruecknahme nur unveraenderter Bloecke mit leerem Behaelter (N13). Kartentisch: Ergebnis-Slot verbraucht genau eine leere Blaupause, Schift-Klick-Schleife endet. **P1-1, P1-2 gefunden und gefixt.** |
| C4 NBT/Komponenten | `BLUEPRINT_ROTATION` `intRange(0,3)`; `BlueprintJobs` mit optionalen Feldern, Index/Drehung beim Laden begrenzt; Baustab-NBT `Length` nicht begrenzt (P3-5); persistenter Blaupausen-Codec ohne Laengengrenze (P3-8). |
| C5 Tick-Kosten | Pruefung <= 131 072 Stellen je Klick-Tick und Spieler; Bau bis 32 768 Stellen je Tick (P2-1); Scan 262 144 Stellen je Tick je offenem Tisch, nur geladene Chunks; Oktant-Fuellung lehnt ungeladene Chunks ab und laedt nichts nach. |
| C6 Leaks | `WandUndo.RECORDS`, `BlueprintBuilder` (JOBS, CHECKS, CLICK_VISITS, HINTED, WARNED): Abmelden und Serverstopp raeumen; Caches begrenzt (Order 4, Parse 24/8 MB, Hash 8); Signier-Budget `WeakHashMap`. |
| C7 Schutz/Rechte | Jede Stelle: Welthoehe, Weltgrenze, geladen, `mayInteract` (Spawnschutz), `mayUseItemAt`, framework-`Protection` (Admin nur LEVEL_OWNERS), SimpleLib-Partnerrechte, Fabric-Abbau-Ereignis (Claim-Mods), Blacklist-Tag; Bett-/Truhen-Gegenstueck mitgeprueft. Befehls-/Strukturbloecke nur fuer Ops. Kein eigener Befehl (Config-Befehl ueber SimpleLib). Mixins: Kartentisch-Slots (Prioritaet 1100, nur Scan-Paare), `broadcastChanges`-HEAD nur fuer den Tisch, Kessel-Accessor; kein `@Overwrite`, Client-Mixins nur im `client`-Block. Schalter `blueprint.enabled` greift nicht ueberall (P3-2). |
| C8 Unabhaengigkeit | Pflicht nur SimpleLib (`fabric.mod.json`); SimpleBuilding/Ores/Storage/Hammers ueber IDs, SimpleLib-Vertraege und `suggests`; Standalone-Gametests (`BtTests`) pruefen Rezepte mit und ohne SimpleBuilding/Simple Ores. |
| C9 Doku/Pflege | Kein TODO/FIXME; Lang EN=DE (354/354 Keys); 59 Kern-Dubletten in `tools/dup_pairs.txt` (P3-11); `DEFAULT_PLACE_BUDGET` ungenutzt (P3-10). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplebuildingtools-P1-1 | **P1 (Dupe)** | `blueprint/BlueprintBuilder.java:571` (vorher): eine kostenlose Haelfte (Bettkopf, obere Tuer-/Pflanzenhaelfte) stand, sobald an der Partnerstelle **derselbe Block** stand - auch ein fremder Bettkopf. Eine signierte Blaupause `red_bed[part=head,facing=north] 0,0,0` neben dem Kopf eines stehenden Betts setzte je Bau einen Gratis-Kopf; Bettkoepfe lassen beim Abbau das Bett fallen (Loot `part=head`). Ebenso gratis obere Haelften von Kleinem Tropfblatt/Hohem Seegras auf einer fremden oberen Haelfte (Schere). | **gefixt**: im Ueberlebensmodus nur auf die passende bezahlte Haelfte (untere Haelfte bzw. Fussteil gleicher Richtung, `paysFor`, `BlueprintBuilder.java:575/623`), Kreativ unveraendert. Gametest `freeBedHeadStandsOnlyAtItsOwnFoot` (+ Paar Kopf/Fuss kostet weiter ein Bett). |
| simplebuildingtools-P1-2 | **P1 (Dupe)** | `util/WandUndo.java:203` (vorher): Rueckgaengig raeumte mit `setBlock(AIR, UPDATE_ALL)`; die andere Haelfte zerfiel per Formupdate **mit Drop**, und das Rueckgaengig erstattete das Item zusaetzlich. Baustab + Bett: Fussteil geraeumt -> Kopf droppt das Bett + 1 Bett erstattet (Bett je Runde verdoppelt). Blaupause + Tuer/hohe Blume: obere Haelfte zuerst geraeumt -> untere droppt Tuer/Blume + Erstattung. | **gefixt**: `clear` (`WandUndo.java:227`) nimmt die passende andere Haelfte zuerst still (ohne Formupdate) und gibt ihren Nachbarn danach das Formupdate. Gametest `undoOfTwoPartBlocksRefundsOnceAndDropsNothing` (Baustab-Bett und Blaupausen-Tuer). |
| simplebuildingtools-P2-1 | P2 (Server-Last) | Bau-Takt bis `blueprint.maxBlocksPerTick` = 32 768 Stellen je Tick (Standard = Maximum, `config/BtConfig.java:24/117`, `BlueprintBuilder.java:1034`): ein Kreativspieler (ohne Op) setzt mit Enderit-Stab + Blaupause oder Oktant-Fuellung 4,2 Mio. Bloecke in 9 s, beliebig oft - auf Kreativ-Servern ein Lag-Hebel (Vanillas `/fill` braucht Op und deckelt bei 32 768). Ueberleben begrenzt das Material. | **Besitzer AUDSB31B F2 = B, umgesetzt (a-audsb31b)**: Tempo bleibt; neue Server-Optionen `blueprint.largeBuildCells` (Standard 32 768, 0 = aus) und `blueprint.largeBuildPermission` (Standard 2 = OP). Ein Gratis-Bau (Kreativ/Kreativ-Stab) von Blaupause oder Oktant-Fuellung mit mehr Stellen braucht die Rechtestufe (`BtTuning.largeBuildDenied` in `BlueprintBuilder.build`/`buildLayout`/`resumeSaved` und `ShapeFill`), rote Aktionsleiste. Bauten mit Material bleiben frei (Abweichung: die Antwort sagt "Bauten", die Frage meinte den Kreativ-Lag-Hebel; Ueberleben begrenzt das Material). Gametest `largeFreeBuildNeedsPermission`. |
| simplebuildingtools-P3-1 | P3 | Edit-Paket: unsigniertes Zwischenspeichern ohne Drossel (bis 32 000 Zeichen je Paket, Rueckmeldung als Slot-Update) - wie Vanillas Buch. | Hinweis |
| simplebuildingtools-P3-2 | P3 | Schalter `blueprint.enabled=false` ("weder scannen noch bauen"): Kartentisch-Scan/Kopie (`BlueprintCartography.java:104`), Fortsetzen eines gespeicherten Auftrags (`BlueprintBuilder.java:1140`) und das Edit-Paket (`ModMessageHandlers.java:225`) fragen ihn nicht. | Vorschlag: `BtTuning.blueprintDenied` an allen drei Stellen |
| simplebuildingtools-P3-3 | P3 | Blaupausen-/Oktant-Bau zeichnet fuer Rueckgaengig immer die Kosten auf (`BlueprintBuilder.java:1005`), `Record.creative` kennt nur den Kreativmodus (`WandUndo.java:97`): mit dem Kreativ-Baustab im Ueberleben erstattet Rueckgaengig nie bezahlte Items. Keine neue Wirkung (der Kreativ-Stab baut ohnehin gratis). | Vorschlag: `creative ? 0 : paid.count()` aufzeichnen |
| simplebuildingtools-P3-4 | P3 | Kopie einer Kreativ-Blaupause am Kartentisch macht aus jeder leeren normalen Blaupause eine Kreativ-Blaupause (`BlueprintCartography.java:58`); sie gibt jedem Stab im Ueberleben 1024 Bloecke Reichweite (`CreativeReach.java:27`). Braucht einmal das Kreativ-Item. | **Besitzer AUDSB31B F6 = B, umgesetzt (a-audsb31b)**: `copyResult(original, blank, player)` gibt die Kreativ-Kopie nur einem Spieler mit `instabuild` (Kreativmodus), sonst kein Ergebnis; Test `creativeBlueprintBuildsFarAndStaysLockedOnceSigned` erweitert (Ueberleben leer, ohne Spieler leer, Kreativ genaue Kopie). Kern-Kopie (26.2) unveraendert, Drift `copyResult/2|3` in `tools/dup_pairs.txt`. |
| simplebuildingtools-P3-5 | P3 | Laufender Bau liest `Mode`/`Length` ungeprueft aus dem Stab-NBT (`BuildingWandItem.java:1047`): ein per Befehl/Kreativ gebauter Stab mit riesigem `Length` erzeugt riesige Stellenlisten je Tick. | Vorschlag: `Length` auf `lineLength(radius)` begrenzen |
| simplebuildingtools-P3-6 | P3 | Bricht der Stab mitten im Ring, setzt `placeWandBlock` (`BuildingWandItem.java:838`) den Rest des Rings noch (kostet Material, keine Gratis-Bloecke). | Hinweis |
| simplebuildingtools-P3-7 | P3 | Kartentisch-Scan liest bis 256^3 Stellen im 32-Block-Umkreis des Tisches, auch verdeckte Erze und fremde Claims (`BlueprintScanner.java:79`) - der Vanilla-Client hat dieselben Daten; relevant nur mit Anti-Xray-Mod. | Hinweis |
| simplebuildingtools-P3-8 | P3 | Persistenter Codec von `BlueprintContent` ohne Laengengrenzen (`BlueprintContent.java:17`), Netz-Codec 32 000/128/64: eine per Befehl erzeugte ueberlange Blaupause liesse sich nicht senden (Kodierfehler/Kick). | Hinweis; **a-fix13b (13.11.)**: Netz-Codec 1 000 000 Zeichen, gespeicherter Code in 20 000er-Stuecken (NBT-String-Grenze 65 535 Bytes), kurze Codes byte-gleich |
| simplebuildingtools-P3-9 | P3 | Client-Klassen ausserhalb `client/`: `version/McClientVersion.java`, `version/TexturedGuiElementState.java`, `util/guiDrawHelper.java` (nur von Client-Code geladen). | Pflege |
| simplebuildingtools-P3-10 | P3 | `BlueprintBuilder.DEFAULT_PLACE_BUDGET` (Zeile 76) ungenutzt, Klassen-Javadoc nennt ihn noch. | Pflege |
| simplebuildingtools-P3-11 | P3 | 59 Kern-Dubletten; die Kern-Kopien haben beide P1: `common/src/shared/java/com/simplebuilding/blueprint/BlueprintBuilder.java:571`, `common/src/shared/java/com/simplebuilding/util/WandUndo.java:204` (26.2, auf 26.3 nicht registriert). Fixes nur mit privaten Methoden -> `check_dup_impl` 0 Probleme. | beim 26.2-Port nachziehen |

## Urteil
**shipbar nach Fix** (2 P1 gefixt, 1 P2 nach Besitzer-Antwort AUDSB31B F2 = B umgesetzt, 11 P3, davon P3-4 nach F6 = B umgesetzt). Payloads, Schutz je Stelle und Materialabrechnung sind durch die
frueheren Audits solide; beide P1 lagen bei zweiteiligen Bloecken (Bett-Loot haengt am Kopf, nicht am Fussteil).

## Testergebnis
Lokal: javac der geaenderten Dateien gegen 26.3 + Fabric-API (`BlueprintBuilder`, `WandUndo`, `BlueprintTests`, `WandModeTests`,
`ModuleGameTest`; nur die bekannte Access-Widener-Meldung in `simplelib/LibMaterials.java`), `modules/simplebuildingtools/tools/check_data.py`
gruen, `tools/check_dup_impl.py` 0 Probleme, Lang EN=DE. Vanilla-Verhalten (Loot-Tabellen Bett/Tuer/Blumen, `Level.setBlock`-Flags,
`AbstractBedBlock#updateShape`) am dekompilierten 26.3-Jar nachgesehen. sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: die beiden neuen Gametests liefen lokal nicht (kein Gradle); Client, Forge/NeoForge, echter Paket-Spam,
Claim-Mods (nur das Fabric-Ereignis).
a-audsb31b (F2, F6): javac von `BtConfig`, `BtTuning`, `BlueprintBuilder`, `ShapeFill`, `BlueprintCartography`, `BlueprintTests`, Adapter lokal sauber
(nur die bekannte Access-Widener-Meldung); sbt siehe `docs/ai/PLAN-A-AUDSB31B-2026-11-12.md`.
