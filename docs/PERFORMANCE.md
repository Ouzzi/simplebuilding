# Leistung und Optimierungs-Mods

Stand 2026-09-28. Gilt fuer alle Linien: 26.2 (`common/src/shared`, Fabric `src/main`, `neoforge/`,
`forge/`), 26.3/26.4 (Overlays, keine eigenen Kopien der hier genannten Klassen) und 1.21.11
(`mc1_21_11/*`, gleiche Logik). Pfade relativ zu `common/src/shared/java/com/simplebuilding/`.

## 1. Audit: was pro Tick / pro Bild laeuft

### Block-Entity-Ticker (Server)

| Block-Entity | Ticker | Befund | Massnahme |
|---|---|---|---|
| Mod-Trichter (`ModHopperBlockEntity`) | Server | Wie Vanillas Trichter (Abklingzeit, `ENABLED`-Frueh-Abbruch vor jeder Arbeit). | unveraendert; siehe Lithium unten |
| Oefen/Schmelz-/Raeucherofen | Server | Erben `AbstractFurnaceBlockEntity` und rufen dessen `serverTick` - Lithiums "schlafende Block-Entities" greifen damit auch hier. | unveraendert |
| Abgelegte Vorlage (`PlacedTemplateBlockEntity`) | Server | Arbeit nur alle 10 Ticks, nach Position versetzt. | unveraendert |
| Chunk-Loader | Server | Arbeit nur alle `CHECK_INTERVAL` Ticks. | unveraendert |
| **Elytra-Pad** | Server | Alle 10 Ticks `getEntitiesOfClass(ServerPlayer, Bereich)`. Stufe V: 128 x 127 x 128 Bloecke = ~512 Entity-Sektionen je Suche, letzte Easter-Stufe ~4096. | `util/PlayerScan`: Spielerliste des Levels statt Sektionssuche - O(Spieler) |
| **Flypad** | Server | dito alle 5 Ticks (bis 16 x 24 x 16, Easter doppelt). | `PlayerScan` |
| **Launchpad** | Server **und** Client | jeden Tick Sektionssuche ueber dem Pad (Client fuer die Partikel). | `PlayerScan` |
| **Spawn-Teleporter** | Server (+ Client-Partikel nur fuer den Besitzer) | jeden Tick Sektionssuche, danach zwei `removeIf`+Stream-Durchlaeufe auch ohne Spieler. | `PlayerScan` + Leerlauf-Abbruch (`isTracking()`) |
| **Trank-Pad** | Server | Waehrend der Abklingzeit jeden Tick `setChanged()` = Chunk markieren **plus** Komparator-Abfrage der vier Nachbarn (das Pad hat kein Komparator-Signal). Sonst jeden Tick Sektionssuche. | `level.blockEntityChanged(pos)` statt `setChanged()`; `PlayerScan` |
| **Kupfer-Druckplatte** | Server | jeden Tick Sektionssuche. | `PlayerScan` |

`getTicker` liefert bereits auf der Client-Seite `null`, wo der Client nichts zu tun hat (alle ausser
Launchpad und Spawn-Teleporter, die Partikel zeichnen). Ein `null`-Ticker fuer Leerlauf-Zustaende
wurde geprueft und verworfen: `getTicker` wird nur bei einem Blockzustands-Wechsel neu gefragt, der
Leerlauf dieser Pads ("niemand steht darauf", "Config aus", "leer") steht aber nicht im Blockzustand.
Dafuer muessten neue Blockzustaende samt Modellen/Datagen her, und ein Pad muesste zuverlaessig
aufwachen, wenn ein Spieler es betritt - das ist die eigentliche Arbeit, die es spart. Die
verbleibenden Kosten je Leerlauf-Tick sind jetzt eine Schleife ueber die Spielerliste.

### Server-Tick-Hooks, Entity-Hooks, Items

Geprueft und ohne Befund (schon versetzt, gedrosselt oder mit Frueh-Abbruch): `SpawnElytra.serverTick`
(Aufraeumen 41 Slots je Spieler, Rest je Sekunde), `LaunchSafety` (leer = sofort zurueck),
`SledgehammerProgress.tick` (nur alle `VALIDATE/REBROADCAST_TICKS`), `DynamicLightHandler`
(Spieler alle 2 Ticks, Traeger/Rahmen je Entity-Id versetzt, Funken erst wuerfeln, dann lesen),
`TrimAttributeHandler` (je Entity versetzt), `LivingEntityMixin`-Tick-Hooks (Modulo vor jeder
Arbeit), Erzdetektor (je Spieler versetzt, hoechstens 16 Scans je Server-Tick), Magnet
(nur in der Hand, nicht beim Schleichen), Bauplaner/Blaupause (Tick-Budget), Testzentrale (nur mit
geplanter Aufgabe).

### Client

| Stelle | Befund | Massnahme |
|---|---|---|
| **Oktant-Figur** (`client/render/BlockHighlightRenderer`) | Fuer Kugel/Zylinder/Pyramide/Prisma lief in **jedem Bild** die komplette Huellen-Suche: jede Zelle der Box, je Aussenseite 6 Nachbar- und 8 Kantenproben, jede Probe ein `Math.pow`-Praedikat (Zylinder/Pyramide mit AABB-Neuanlage). | `util/OctantSurface`: Huelle einmal berechnen, zwischenspeichern, bis sich Form, Ausrichtung oder eine Ecke aendert. Gezeichnet wird vertexgleich. |
| **Baustab-Vorschau** (`client/render/BuildingWandPreviewRenderer`) | Flaeche/Linie/Bruecke wurden in jedem Bild neu geplant: je Stelle Platzierungszustand, Nachbarformen, Halt und eine Entity-Suche (`isUnobstructed`). Blaupause und Oktant-Fuellung hatten schon einen Cache. | Ergebnis je Spieltick und Eingabe (Treffer, Trefferpunkt, Blickwinkel, Position, Schleichen, Stab + Einstellungen) zwischengespeichert. |
| `PlacedTemplateRenderer` (einziger BER) | 1x1-Block, Standard-Sichtweite 64, nicht `shouldRenderOffScreen` - Vanilla-Sektions-Culling und Entity Culling greifen. | unveraendert (kleinere Sichtweite waere sichtbar: bei 32 Bloecken noch ~24 px breit) |
| Rucksack-/Koecher-Ebene | Nur ein Int-Map-Lookup (`level.getEntity(id)`) je Spieler und Bild, Vanilla-RenderTypes. | unveraendert |
| Laserpunkt, HUDs | Nur beim Zielen/Tragen aktiv; Vanilla-Pipelines (`GUI_TEXTURED`, `lines`, `debugQuads`). | unveraendert |

## 2. Gemessene / geschaetzte Gewinne

Zahlen aus `PerformanceTests` (Log-Kanal `simplebuilding-perf`, Server-Spieltest, siehe dort):

- **Spielersuche**, Bereich Elytra-Pad Stufe V (letzte Easter-Stufe): Sektionssuche 8,9-25,1 us je Aufruf, Spielerliste 0,82-2,1 us - **rund 11-12x schneller** (2000 Durchlaeufe, 3 Mock-Spieler, Fabric 26.2/1.21.11/26.3; in einer Welt mit vielen Entities in den Sektionen waechst der Abstand).
  Pro Pad alle 10 Ticks; die Sektionssuche waechst mit der Bereichsgroesse, die Spielerliste nur
  mit der Spielerzahl.
- **Oktant-Kugel 32^3**: **175 280** Praedikataufrufe (4872 Aussenseiten, 11 520 Kanten), frueher **in jedem Bild**, jetzt einmal je
  Aenderung der Auswahl (bei 144 FPS vorher ~25 Mio. Aufrufe je Sekunde).
- **Baustab-Vorschau**: bei ruhigem Blick und 144 FPS rund 7 von 8 Planungen gespart (eine je Tick
  statt je Bild); bei bewegter Maus wie vorher.
- **Trank-Pad in Abklingzeit**: 4-8 `getBlockState` + Komparator-Pruefungen weniger je Tick und Pad.
- **Spawn-Teleporter im Leerlauf**: zwei `removeIf`-Durchlaeufe mit Stream-Lambdas und die
  Stufen-/Easter-Abfrage weniger je Tick und Teleporter.

Verhalten unveraendert: `PlayerScan` findet genau die Spieler der Sektionssuche (gleiche Bedingung
`getBoundingBox().intersects(box)`; nur die - dort ebenfalls nicht festgelegte - Reihenfolge kann
abweichen), die Oktant-Huelle ist Seite fuer Seite und Kante fuer Kante die der alten Schleife
(beides per Spieltest belegt). Die Baustab-Vorschau kann Welt- oder Inventaraenderungen innerhalb
desselben Ticks hoechstens einen Tick (50 ms) spaeter zeigen.

## 3. Optimierungs-Mods: Kompatibilitaet

Verfuegbarkeit laut Modrinth (Stand 2026-09-28). Fuer Forge gibt es auf 1.21.11/26.x keinen der
Mods; die Forge-Linie der Mod ist davon nicht betroffen.

| Mod | Loader / Linien | Status mit SimpleBuilding | Hinweise |
|---|---|---|---|
| Sodium | Fabric + NeoForge, 1.21.11 bis 26.3 | kompatibel | Keine Mixins in Sodium-ersetzte Klassen (`SectionCompiler`, `SectionRenderDispatcher`, Terrain-Teile des `LevelRenderer`). Blockmodelle sind Vanilla-JSON, Tints ueber `BlockColors` (Rucksack), einziger BER nutzt die Submit-Pipeline. Die Forge-`LevelRendererMixin`s laufen nur auf Forge, wo es kein Sodium gibt. |
| Embeddium / Rubidium | tot (letzte Versionen 2025/2023) | - | Sodium hat seit 0.6 einen eigenen NeoForge-Build. |
| Iris | Fabric + NeoForge (26.3 bisher nur Fabric) | kompatibel (visuell pruefen) | Keine eigenen `RenderPipeline`s: nur Vanilla-`RenderTypes` (`lines`, `debugQuads`, `translucentMovingBlock`, `entityCutout`) und `RenderPipelines.GUI_TEXTURED`. Eigene Pipelines muessten sonst per `IrisApi.assignPipeline` angemeldet werden. |
| Lithium | Fabric + NeoForge, alle Linien | kompatibel | Lithiums Trichter-Optimierung greift nur fuer Vanillas `HopperBlockEntity`; die Mod-Trichter gehen den Vanilla-Weg und rufen `setChanged()` nach jedem Transfer. Mod-Oefen erben `AbstractFurnaceBlockEntity` und schlafen mit Lithium wie Vanilla-Oefen (Zusatz-Kochticks laufen nur, waehrend der Ofen brennt, also nie im Schlaf). Keine `@Overwrite`/`@Redirect` auf Entity-Bewegung/-Kollision oder Trichter-Statics. Kein `lithium:options`-Opt-out noetig. |
| Canary / Radium | tot seit 2024 | - | Lithium laeuft selbst auf NeoForge. |
| FerriteCore | Fabric + NeoForge, alle Linien | kompatibel | `BlockStateBaseMixin` injiziert nur in `getDestroyProgress` (kein Feldzugriff auf Zustandstabellen). |
| ModernFix | nur NeoForge 26.1.2 | kompatibel | Kein Einlesen gebackener Modelle beim Start. |
| Entity Culling (tr7zw) | Fabric + NeoForge, alle Linien | kompatibel, **keine Whitelist noetig** | Culling per Sichtstrahl gegen die Render-Box. Der einzige BER (abgelegte Vorlage) zeichnet nur in seinem Block; Laser, Oktant- und Stab-Vorschau sind keine (Block-)Entities; Rucksack/Koecher werden zusammen mit dem Spieler verdeckt, was richtig ist. Die schwebende Sand-Entity erbt `FallingBlockEntity` und wird wie fallender Sand behandelt. Sollte spaeter ein BER ueber seinen Block hinaus zeichnen (Strahl o. ae.): `shouldRenderOffScreen() = true` - dann culled Entity Culling ihn nie - oder die NeoForge-`getRenderBoundingBox` / Fabric-`BlockEntityRenderFabricExtension` aus einer optional geladenen Compat-Klasse (nie direkt referenzieren, vgl. Entity-Culling-Issue #337). |
| ImmediatelyFast | Fabric + NeoForge, alle Linien | kompatibel | HUD nur ueber `GuiGraphicsExtractor` mit Vanilla-Pipelines, kein roher GL-/`RenderSystem`-Zustand zwischen Zeichenaufrufen. |
| More Culling | Fabric + NeoForge (26.3 Beta) | kompatibel | Culled Schilder, Rahmen, Leuchtfeuer-Strahlen; die Mod hat keinen Strahl-BER. `ItemFrameEntityRendererMixin` (verborgene Rahmen) sitzt in `extractRenderState` - bei aktivem Rahmen-Culling von More Culling visuell pruefen. |
| Enhanced Block Entities | nur Fabric bis 1.21.4 | nicht verfuegbar | Ersetzt ohnehin nur Vanilla-BERs (Truhen, Schilder ...). |
| C2ME | Fabric (+ NeoForge-Port), alle Linien | kompatibel | Weltgen der Mod (Erze) nutzt Vanilla-Features ohne gemeinsamen Zustand. |
| Krypton | Fabric bis 26.2 | kompatibel | Keine eigenen Netty-Handler, nur Payloads ueber die Loader-APIs. |
| Noisium | archiviert (Nov. 2025) | - | - |
| Sodium Extra, Debugify | Fabric (+ NeoForge fuer Sodium Extra) | kompatibel | - |
| Nvidium | Fabric | kompatibel | Ersetzt nur Terrain; kein Iris gleichzeitig. |
| Distant Horizons | Fabric + NeoForge | kompatibel | LODs zeigen nur Terrain - Block-Entities der Mod erscheinen erst in normaler Sichtweite (wie alle BERs). |
| ScalableLux (Starlight-Nachfolger) | Fabric + NeoForge (Alpha) | kompatibel | Keine Mixins in die Licht-Engine; dynamisches Licht setzt echte Lichtbloecke ueber `setBlock`. |
| VMP (Very Many Players) | Fabric | kompatibel | Keine Mixins in `ChunkMap`/Entity-Tracking. |
| REI (Roughly Enough Items) | Fabric + NeoForge 26.2 (26.2.820) und 1.21.11 (21.11.816); kein Build fuer Forge 26.2, 26.3, 26.4 | kompatibel, **eigenes Plugin** (optional) | Dieselben Inhalte wie das JEI-Plugin: sieben Umwandlungs-Kategorien und "Mob-Drops" (aus `InWorldRecipeCatalog`/`MobDropCatalog`), Schmiederezepte mit Mengenangabe (`count_based_smithing`, als REI-`DefaultSmithingDisplay` ueber einen serverseitigen Rezept-Filler), Infoseiten der rezeptlosen Gegenstaende. Die Easter-Schmiedekette bleibt versteckt. Quellen `common/src/rei/java` + `mc1_21_11/rei/java`, NeoForge-Anmeldung in `neoforge/src/rei/java` bzw. `mc1_21_11/neoforge/src/rei/java`; `:forge`, `:mc26_3:*`, `:mc26_4:fabric` kompilieren den Baum nicht. Kein Dev-Laufzeit-Mod neben JEI. |
| EMI | nur bis 1.21.1 (Fabric/NeoForge/Forge) | nicht verfuegbar | Weder Modrinth noch maven.terraformersmc.com haben einen Build fuer 1.21.11, 26.2 oder 26.3 (geprueft 2026-09-28) - deshalb kein EMI-Plugin. Der Tag `c:hidden_from_recipe_viewers` ist schon gesetzt; sobald es EMI fuer eine Linie gibt, kann ein Plugin dieselben Kataloge nutzen. |

### Mixin-Ziele mit Konfliktpotenzial

Alle Mod-Mixins wurden gegen die Hotpaths der Mods oben gelesen. `@Redirect` gibt es nur noch in
`BowItemMixin` (`Player#getProjectile`, Koecher) und `HudExperienceLevelMixin`
(`ContextualBar#extractExperienceLevel`) - beides keine Ziele der genannten Optimierungs-Mods.
Tick-Hooks (`LivingEntity#tick`, `Player#tick`, `ItemEntity#tick`, `ExperienceOrb#tick`) sind
`@Inject` an `TAIL`/`HEAD` und vertragen sich mit Lithiums Entity-Optimierungen.

### Quellen

Entity Culling: github.com/tr7zw/EntityCulling (CullTask, EntityCullingModBase,
BlockEntityRenderFabricExtension, Issue #337); Sodium: github.com/CaffeineMC/sodium
(`net.caffeinemc.mods.sodium.api`); Lithium: github.com/CaffeineMC/lithium
(lithium-fabric-mixin-config.md, Wiki "Disabling Lithium's Mixins", Issues #417/#444);
ImmediatelyFast: github.com/RaphiMC/ImmediatelyFast (Issues #419, #579); Iris: malilib-Issue #165
(`IrisApi.assignPipeline`); More Culling: github.com/fxmorin/moreculling; Verfuegbarkeit:
api.modrinth.com/v2/project/{slug}/version.
