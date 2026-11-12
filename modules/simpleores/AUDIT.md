# Audit simpleores (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric. Aufbau wie `modules/simplestorage/AUDIT.md` und `modules/simplepads/AUDIT.md`.
Umfang des Mods: 26 Java-Dateien / 2,4k Zeilen (ohne Tests, ohne Forge/NeoForge - nicht Teil des Releases; davon 657 Zeilen erzeugt: `OresGuide`,
`OresDiscover`), Tests 1,6k Zeilen. Ein Mixin (`SoundproofMixin`), keine Menues, keine Block-Entities, keine Payloads, keine Befehle ausser
`/simpleores config` (SimpleLib). Werkzeuge und Ruestung der beiden Rosen-Legierungen sind reine Vanilla-Materialien ohne Sondereffekt.

## Vollstaendig gelesen
- Mixin und Klang: `mixin/SoundproofMixin.java`, `sound/Soundproofing.java` (Weg-Berechnung, Paketfilter), `OresSounds.java`, Tags `soundproofing*`.
- Material/Logik: `OresMaterials.java` (Werkzeug- und Ruestungswerte), `OresRegistry.java` (alle 130+ Bloecke/Items, Eigenschaften, Feuerfestigkeit),
  `OresFood.java`, `item/FoodEffectsItem.java`, `item/SageOrbItem.java`, `OresLoot.java`, `SimpleOres.java` (IdMoves), `config/OresConfig.java`.
- Bloecke: `block/DimensionalScrapBlock.java`, `LeadPlateBlock.java`, `SilverSheetBlock.java`, `SilverMirrorBlock.java`, `MetalDoorBlock.java`,
  `MetalTrapDoorBlock.java`, `MetalBarsBlock.java`, `MetalGrateBlock.java`.
- Weltgenerierung: `RoseGoldVeinFeature.java`, `MiniGeodeFeature.java`, `fabric/.../OresFabric.java` (Biomfilter, Schalter), `OresFabricClient.java`, `OresViewer.java`.
- Daten: `check_data.py` (Texte, Beute, Rezepte, Weltgen-Zahlen); alle 131 Beute-Tabellen per Skript auf Pool-/Item-Zuordnung geprueft (kein Doppel-Drop).
- Gegengelesen in den Minecraft-Quellen 26.3: `ServerCommonPacketListenerImpl#send`, `AbstractArrow`, `Level#setBlock`.
- Nicht gelesen: `guide/OresGuide.java`, `discover/OresDiscover.java` (erzeugt), Worldgen-/Rezept-JSON im Einzelnen (nur ueber `check_data.py`), Tests, Forge/NeoForge.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigene Payload. Das Mixin ersetzt nur ausgehende Klangpakete (`ClientboundSoundPacket`, `...SoundEntityPacket`, `...ExplodePacket`) durch leisere Kopien oder gar nichts; Groesse wird nie vergroessert (Explosion: 1 stumm + 1 Klangpaket). |
| C2 Server-Autoritaet | Alles serverseitig: Klangweg, Sage-Orb-Erfahrung (Server wuerfelt 50-100), Dimensional-Scrap-Abbau (`getDestroyProgress` prueft die Haupthand; Vanilla rechnet den Fortschritt am Server nach, ohne Werkzeug 0), Config geklemmt (A9). Client-Klassen nur in `OresFabricClient` (leer). |
| C3 Dupes/Verlust | Keine Container. Plattenstapel (`LeadPlateBlock`): Schicht +1 nur mit demselben Item auf der freien Seite (`canBeReplaced`), Beute = Schichten (`layers` 1-8 in der Tabelle), verbraucht 1 Item je Schicht; Fluessigkeit spuelt die Platte weg wie Teppich (Beute bleibt). Tueren geben nur mit `half=lower` Beute. Erze: Silk-Touch/Fortune-Tabellen ohne zweiten Pool. Sage-Orb: `consume(1)` wie Vanilla (Kreativ verbraucht nicht). |
| C4 NBT/Komponenten | Keine Block-Entities; Zustaende `FACING`, `LAYERS` 1-8; alte Platten ohne `layers` laden als eine Schicht; Spiegel ohne `face` als Wandspiegel; `IdMoves` fuer Blossom->Rose, Enderite (SimpleLib) und die abgeschafften Erz-Zwillinge. |
| C5 Tick-Kosten | Kein Tick. Klangfilter: je Klangpaket (Lautstaerke ab 0,3) ein 3D-DDA ueber hoechstens 24 Zellen mit `hasChunkAt` (laedt nichts) und `getBlockState`; alle anderen Pakete kosten drei `instanceof`. Weltgen: `MiniGeodeFeature` liest einen Ball (Radius-Quadrat 11, 171 Zellen) und platziert nur, wenn alles Gestein ist. |
| C6 Leaks | Nur `ThreadLocal`s (`Soundproofing.SENDING`, `RoseGoldVeinFeature.PLACED`), beide im `finally` zurueckgesetzt; keine statischen Maps mit Spielern/Entities/Leveln. |
| C7 Sicherheit/Rechte | Mixin: Ziel `ServerCommonPacketListenerImpl#send(Packet, ChannelFutureListener)`, `@Inject` am Kopf, abbrechbar, `defaultRequire 1`, keine @Overwrite, gleiche Prioritaet 1000; nur fuer `ServerGamePacketListenerImpl`, Wiedereintritt per `SENDING` gesperrt. Bei `soundproofing=false` gibt `filter` sofort `null` (Paket unveraendert). Dimensional Scrap: Widerstand 3 600 000, `IMMOVEABLE` (Kolben), nur Tag `breaks_dimensional_scrap`. Befehl `/simpleores config` nur Stufe 2. Kein Weltumbau durch Items/Bloecke. |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel `runSimpleoresStandaloneGameTest`), nur `framework` + `simplelib`; Octets, Hammers, SimpleBuilding nur ueber Tags mit `required: false`. |
| C9 Doku/Pflege | Kein TODO/FIXME. Lang EN/DE gleich, amerikanisches Englisch geprueft. Kern-Dubletten: `DimensionalScrapBlock`, `SageOrbItem` (`check_dup_impl` 0 Probleme). |

## Befunde
Keine P1, keine P2.

| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleores-P3-1 | P3 | `SoundproofMixin` (SoundproofMixin.java:15-26) gibt jedes ausgehende Paket durch `Soundproofing.filter`; die Kosten sind klein (siehe C5), aber nie gemessen. Der Filter liest Level und Entities (`player.level().getEntity`) und setzt voraus, dass Klangpakete im Server-Thread gesendet werden (Vanilla tut das). | Hinweis; Messung mit vielen Spielern und Klangdichte fehlt |
| simpleores-P3-2 | P3 | Bei mehreren Ersatzpaketen (Explosion: stumm + Klang) wird derselbe `ChannelFutureListener` an jedes weitergereicht, bei leerem Ergebnis nie aufgerufen (SoundproofMixin.java:24). Klang-/Explosionspakete werden in Vanilla ohne Listener gesendet, daher ohne Wirkung. | Hinweis |
| simpleores-P3-3 | P3 | Der Bleiwand-Filter haengt nur an `OresConfig.soundproofing` (Soundproofing.java:128), nicht am Sub-Mod-Schalter "Enable Simple Ores": mit dem Schalter aus bleibt die Daempfung aktiv ("Bloecke und Items bleiben funktionsfaehig"). | Hinweis (bewusst, Wiki-Text pruefen) |
| simpleores-P3-4 | P3 | Der Weg wird nur ueber die ersten 24 Zellen ab dem Geraeusch untersucht (Soundproofing.java:73): eine Bleiwand direkt am Ohr bei einem Geraeusch ueber 24 Zellen entfernt wird nicht gezaehlt. Im Kopf-Kommentar dokumentiert; Spieler hoeren dann zu viel, nie zu wenig. | Hinweis |
| simpleores-P3-5 | P3 | 2 Kern-Dubletten (`DimensionalScrapBlock`, `SageOrbItem`, 26.2-Kern); keine Aenderung in diesem Audit. | Liste |

## Urteil
**shipbar** (0 P1, 0 P2, 5 P3). Der Mod ist reine Daten-/Block-/Item-Logik mit einem schmalen, gut abgesicherten Mixin; Werkzeuge, Ruestung und
Aepfel verwenden Vanilla-Mechaniken ohne Sondereffekte ausser den dokumentierten Apfel-Statuseffekten (Zahlen vom Besitzer freigegeben).

## Testergebnis
Lokal: keine Dateien des Mods geaendert, daher kein javac-Lauf noetig; `modules/simpleores/tools/check_data.py` gruen (1620 Dateien), `tools/check_dup_impl.py` 0 Probleme.
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: Klangweg und Bleiwand im echten Spiel mit Mehrspieler-Last, Weltgeneration (nur Quellen und Zahlen gelesen), Forge/NeoForge, Client.
