# Review 2, Gruppe A (24.10.2026, Branch `b-rev2a24`)

Auftrag Besitzer: Sub-Mods fertig machen, jede Datei einzeln auf Vollständigkeit und Probleme prüfen, dazu
Verbesserungsvorschläge. Nur MC 26.3 + Fabric (NeoForge-/Forge-Quellsätze nicht geprüft). Plan:
`docs/ai/PLAN-B-REV2A-2026-10-24.md`. Vorhandenes nicht doppelt: REV24 (`REVIEW-CODE-2026-10-24.md`, für Gruppe A
H-2, H-4, M-4, M-11, N-2, N-5, N-7, N-8 laut Queue behoben), Modul-Audit `AUDIT-MODULE-2026-10-24.md`
(Branch b-audmods24), JEI (b-jei24), Config-Befehle (CMD24).

Methode je Modul: (1) Werkzeug-Durchlauf über jede Datei (JSON parsebar, TODO/Platzhalter, `@Redirect`/`@Overwrite`,
breite/stumme `catch`, Client-Importe außerhalb des Client-Teils, statische Sammlungen ohne Aufräumen, Importe über
Modulgrenzen, Threads, Netz-Empfänger, `getBlockState` ohne Chunk-Prüfung), dazu `tools/audit_data.py` (Lang, Rezepte,
Loot, Tags, Advancements) und neu `tools/check_lang_keys.py`; (2) Lesen der Java-Dateien (Server-/Logikpfad vollständig,
Client auf Seiten-Trennung), Mixin-Configs gegen Klassen, `fabric.mod.json` gegen Entrypoints/Abhängigkeiten, Daten
gegen Registry-Ids. Generierte Schablonen-Dateien (`*Guide.java`, `*Discover.java`, `DiscoverTipMixin`) wurden einmal an
der Vorlage (`tools/guides/ModuleGuide.java.in`, `tools/discover/*.in`) und per Diff gegen die Vorlage geprüft.

Schwere: P1 Absturz/Datenverlust/Exploit, P2 Fehlverhalten, P3 Qualität, P4 Vorschlag.
Dateizahlen: Fabric + shared + clienttest + tools + wiki (ohne `build/`, `neoforge/`, `forge/`, `generated/`).

## simpleaesthetics (15 Dateien, 5 Java)
Super-Mod ohne Inhalt: Config (`ConfigFiles.readOrQuarantine` + `writeAtomic`), `SubMods`-Schalter, 2 GameTests.
- Kein Fund. P4: Config-Labels ohne Tooltip (liegt bei b-cmdcfg24, AUDM24-5). Test `configFile` lässt sein Temp-Verzeichnis liegen (harmlos).

## simplebeacons (87 Dateien, 34 Java, 39 JSON)
Geprüft: Registry, Config (Klammern aller Zahlen), Strong Beacon (BE, Logik, Menü mit Server-Validierung, Range-Faktor),
Strong Conduit, In-World-Upgrade (Rechte `mayBuild`/`mayInteract`, Stern je Schlag), 7 Mixins + 4 Client-Mixins (alle im
richtigen Abschnitt), Bildschirm, Rezepte (Load-Conditions für `simplemobs:nether_heart`), Loot, Lang EN=DE.
- **P3 behoben** `mixin/BeaconRangeMixin.java`: HEAD schrieb das statische Prozent auch vom Client-Thread (der Client ruft
  `applyEffects` auch auf). Im Einzelspieler konnte das zwischen HEAD und `inflate` des Server-Threads die Reichweite auf 100 %
  zurücksetzen. Jetzt schreibt nur die Server-Seite.
- **P3 behoben** `upgrade/StrongUpgrades.finish`: fehlte beim letzten Schlag der Netherstern (vorher weggelegt), brach es still ab;
  jetzt derselbe Bruch-Sound wie beim ersten Schlag ohne Stern.
- P4 Reichweite bis (50 + 20 × 6) × 2 = 340 Blöcke per Config: `getEntitiesOfClass` über eine sehr große Box alle 80 Ticks; Config-Hinweis
  oder Obergrenze 2× Vanilla-Box. (REV2-A-1)

## Daten- und Text-Prüfungen (alle 14 Module)
- `tools/check_missing_textures.py`: 0 Fehler. `check_empty_tags.py`, `check_loot_format.py`, `check_load_conditions.py`: grün.
- Eigene Ids in Rezepten/Loot/Tags der 14 Module gegen Item-/Blockstate-/Tag-Dateien: 0 echte Lücken (16 Treffer waren Fluide,
  Biome, Tränke, Handels-Ids – geprüft, vorhanden).
- `tools/audit_data.py`: 2× britisches Englisch in Guide-Seiten (`simpleblocks.guide.page.7` „colours“, `simpleend.guide.page.6`) –
  Guide-Texte werden aus `wiki/manual.json` erzeugt (REV2-A-2). Rest der Meldungen (unerreichbare Eimer-Zustände, Endblöcke aus der
  Weltgenerierung, Wandschilder ohne eigene Loot-Tabelle) sind gewollt (Zustände/`dropsLike`).
- **Neu** `tools/check_lang_keys.py` (in `checkModuleData`): jeder wörtliche Lang-Schlüssel im eigenen Namensraum in Modul-Java
  braucht EN und DE. Fand den P2 bei simplemaps (unten); sonst 0 Lücken in allen 44 Modulen.

## simpleblocks (726 Dateien, 32 Java, 671 JSON)
Geprüft: Registry (Eigenschaften von Vanilla kopiert, gealterte Ziegel), IdMoves (alte `simplebuilding:`-Ids, Säge-Umbenennung),
Natur-Blöcke (schwebender/levitierender Sand mit eigener Entity, Gras-/Erd-Stufen mit Ausbreitung, fallende Stufen, rissiges Eis +
2 Mixins), Shapecutter (Menü wie Vanilla-Steinsäge, Rezeptliste Server→Client, Index-Gleichheit), Loot-Injektion, Kreativ-Tabs.
- **P3 behoben** `saw/SawUpgrade.tryUpgrade`: Steinsäge → Shapecutter ging auch im Abenteuermodus/als Zuschauer (Block tauschen ohne
  `mayBuild`); jetzt verweigert, GameTest-Fall im Pfad `saw_upgrade_axe/hammer` ergänzt. (Spawn-Schutz prüft Vanilla schon vor dem
  Use-Event.)
- P4 `saw/Sawing`: `clientEntries` überlebt einen Serverwechsel, `cachedManager` hält den alten RecipeManager bis zur nächsten Welt;
  bei Disconnect leeren. Paketgröße der Liste wächst mit jedem Steinsägen-Rezept (heute weit unter 1 MiB). (REV2-A-3)

## simplecrucibles (314 Dateien, 72 Java, 151 JSON)
Geprüft: Tiegel-BE (Reservierung, Sperren, Ergebnis-Schutz, Erfahrung, Speichern/Laden mit Slot+Count, Upgrade-Schnappschuss),
Menü (doppelte Layouts, Geister-Slots: `mayPlace`/`mayPickup` false, auch gegen modifizierte Clients), Fass (Andocken, Abdocken mit
Überlauf-Drop), Rohling/Bau, Upgrades (Axt/Hammer, Kessel-Zähler mit Aufräumen bei >256), Hitze, Rezeptsuche, Eimer (Kupfer/Keramik/
Enderit, Schöpfen mit `mayInteract`/`mayUseItemAt`), Seelenlava (Fluid, Brunnen/Quellen-Mixins), 10 + 1 Mixins (Client-Mixin im
`client`-Abschnitt), Config (alle Werte geklammert, Altdateien übernommen), Dorf-Feldküche, API.
- REV24 H-4 Gegenprobe: GameTest `load_keeps_readable_slots_around_an_unreadable_one` ist im Gate grün (26.3 liest die Liste
  eintragsweise) – kein Fund mehr.
- **P3 behoben** `mixin/AxeBuildMixin`: der Axt-Weg (Tiegel bauen, Fass andocken, Tiegel/Fass/Kessel aufwerten) lief am Kopf von
  `ItemStack.useOn`, also vor Vanillas Abenteuer-Prüfung – im Abenteuermodus ließ sich bauen. Jetzt `mayBuild` vorher; Prüfung im
  GameTest `axe_builds_iron_crucible` ergänzt.
- P4 `crucible/CrucibleBlockEntity.loadAdditional` / Fass: Mengen werden beim Laden auf das Stapel-Limit gekürzt; die Multiplikatoren
  sind fest, aber ein Datapack, das `max_stack_size` senkt, löscht still den Überschuss (besser: beim nächsten Tick auswerfen). (REV2-A-4)
- P4 `village/VillageKitchen.inject`: Feldküche kommt auch bei ausgeschaltetem Sub-Mod (`SimpleCrucibles.on()`) in die Dorf-Pools. (REV2-A-4)
- P4 `fluid/CrucibleBucketItem.emptyContents` (Kupfer-Wasser): Ausgießen auf eine Wasserquelle ersetzt sie durch fließendes Wasser
  (Quelle geht verloren). (REV2-A-4)
- P4 `mixin/CopperBucketFrameCareMixin`: Wachsen/Abkratzen eines gerahmten Kupfereimers prüft weder festen Rahmen (`fixed`) noch
  Bau-Rechte. (REV2-A-4)
- P4 `test/CrucibleTests` importiert `com.simplelib.registry.LibComponents` (intern, nur Test). (REV2-A-4)

## simpledimensions (80 Dateien, 43 Java, 23 JSON)
Geprüft: Laufzeit (Zünden mit `mayInteract` + Claims/`Protection` für Rahmen und Innenraum, Reise mit Rückadresse, Zielinsel nur
nach vollständiger Vorprüfung, Rückbau bei fehlgeschlagenem Teleport, Rahmen-Zerfall gedrosselt 8/Tick, Notrückkehr), Config
(Grenzen, Symlink-/Größenprüfung, atomares Schreiben), Datapack-Erzeugung, Portal-Block/-BE, Rahmen-Scanner/-Rezepte, 2 Mixins,
5 JUnit-Tests + 33 GameTests. Discover hat keine Tipps (leere Liste), daher bewusst kein DiscoverTipMixin.
- **P3 behoben** `DimensionRuntime.tickPlayer`: scheiterte die Notrückkehr (Spieler in entfernter/kaputter Dimension, kein sicherer
  Platz), lief sie jeden Tick erneut: Chunk-Ladungen und bis ~3000 Platzprüfungen pro Tick und Spieler. Jetzt höchstens einmal je
  Sekunde (`Contact.rescueAt`).
- P4 `DimensionRuntime.save` wirft `IllegalStateException` bei Schreibfehler (volle Platte) mitten im Server-Tick → Server-Absturz;
  besser loggen und beim nächsten Mal erneut speichern. (REV2-A-5)
- P4 `DimensionRuntime.CONFIG_ROOT = Path.of("config/simpledimension")` hängt am Arbeitsverzeichnis statt am Loader-Config-Ordner
  (Launcher mit anderem gameDir); Umzug nur mit Migration. (REV2-A-5)
- P4 `DimensionConfigStore.read` meldet verworfene Definitionen über `System.err`, `DimensionPortalConfig` verwirft kaputte Rezepte
  ganz still – beides über den Logger melden. (REV2-A-5)
- P4 `SkyPortalBlockEntity.getUpdateTag` schickt Ziel-/Rückkehr-Koordinaten (`Link`, `Anchor`) an jeden Client in Reichweite; der
  Client braucht nur die Farbe. (REV2-A-5)
- P3 `ExampleItems` ist eine leere Hülle ohne Aufrufer (toter Code). (REV2-A-5)

## simpleend (249 Dateien, 41 Java, 130 JSON)
Geprüft: Registry/Holz-Familien (Schilder/Hängeschilder/Regal über `BlockEntityTypeMixin`), Laub (Dichte, Wachs, Spinnennetz-
Bremse), Knospen + Bäume (Schreiben nur in geladene Chunks), Stöcke (Einsturz mit `isLoaded` und Obergrenze 8192), Nibblemite
(Zähmen, Modus, Zucht), Drift-Effekt (WeakHashMap je Seite), Biome (Resolver-Wrapper, Server-Registry), Leere-sichere Items,
Config (Quarantäne, Klammern), 9 + 2 Client-Mixins, Lang (EN=DE).
- **P2 behoben** `entity/Nibblemite.nibble`: verwandelte End-Stein auch bei `mobGriefing=false` in angeknabberten End-Stein (Vanilla-
  Schaf frisst dann kein Gras). Jetzt wie Vanilla: gefüttert wird, der Block bleibt. GameTest `nibblemite_nibbles_end_stone` prüft beide Fälle.
- **P3 behoben** britisches Englisch in Spielertexten („neighbours“ in Guide/Wiki/Fragebogen) → amerikanisch (Regel 5); ebenso simpleblocks „colours“.
- P4 `mixin/ItemEntityMixin`: leere-sichere Items bleiben nach dem Anheben dauerhaft `noGravity` am Weltboden schweben (gewollt), werden
  aber nie wieder normal fallen, wenn darunter später Boden gebaut wird – Schwerkraft zurückgeben, sobald `y` über dem Boden liegt. (REV2-A-6)

## simpleenddragonfight (54 Dateien, 18 Java, 22 JSON)
Geprüft: Kampf-Sitzung (Teilnehmer mit Gnadenfrist, Skalierung über Attribut bis 1024 + Schadensfaktor, Kristall-Heilung, Wellen
mit Obergrenze lebender Milben, Einzugs-Animation rein kosmetisch), Beschwörungs-Zähler (SavedData), Dragonmite (löst sich auf,
wenn sein Drache weg ist), 2 Mixins (`@ModifyArg`/`@WrapOperation`/`@ModifyVariable`, kein Redirect), Config (alle Zahlen geklammert).
- Kein P1–P3. P4 `DragonFightServer.END` ist statisch: Warteschlange (hält `Player`-Objekte), Teilnehmerliste und Faktoren überleben
  einen Weltwechsel im Einzelspieler bis zum ersten Tick der neuen Welt; bei `SERVER_STOPPED` zurücksetzen (PlayerState-Hook aus
  REV24-N-1 nutzen). (REV2-A-7)

## simplefarming (30 Dateien, 19 Java, 6 JSON)
Geprüft: Hacken-Ernte (Reife, Saat bezahlt, Rechte über `InteractionGuard` inkl. `mayBuild`/`mayInteract`/`mayUseItemAt`, Drossel 4 Ticks),
Schärfe-Grasschnitt (nur Server, max. 27 Blöcke, je Block Rechteprobe), Ackerschutz (Federfall), Config + Sync-Paket (32 KiB-Grenze,
Fehler ignoriert, beim Trennen geleert), Befehle (Rechte-Stufe Owner), 3 Mixins, Config-Bildschirm.
- **P3 behoben** `mixin/GrassOutlineMixin`: hängt an `BlockStateBase.getShape` (jeder Strahl, jede Umrandung); mit Schärfe-Schwert in der
  Hand lief für jeden Block eine Registry-/Verzauberungs-Abfrage. Jetzt wird erst geprüft, ob der Block überhaupt schneidbar ist.
- P4 `FarmingFabric`: `SimpleFarming.onChange` hält nach Server-Stopp den alten `MinecraftServer` (Einzelspieler-Weltwechsel) bis zum
  nächsten Start; bei `SERVER_STOPPED` zurücksetzen. (REV2-A-8)
- P4 Guard-Duplikat `InteractionGuard` (REV24-N-5, Teilstand b-revn5) – hier noch die Modul-Kopie. (REV2-A-8)

## simplefun (89 Dateien, 46 Java, 24 JSON)
Geprüft: 7 + 1 (Fabric) + 3 Client-Mixins, Ziegel-Wurf (Abklingzeit Server + Item, Lebensdauer 200 Ticks, Glas nur im Einzelspieler
ohne LAN und mit `mayInteract`), Federn/„Schadlos“ (nur Spieler-Angriff), Rückstoß-Kappe, Yeet (nur Schleich-Wurf lebend, Tempo
gekappt), Kleine Freuden (Abklingzeit, Partikel-Grenze), Befehle (Stufe 4), Config (Klammern), JEI-Plugin (nicht angefasst), Tests.
- **P3 behoben** `mixin/PiggyFoodMixin`: der Schweinchen-Effekt kam auch bei ausgeschaltetem Sub-Mod (`SimplefunCommon.on()` fehlte);
  Prüfung in `sub_mod_switch` ergänzt.
- **P3 behoben** `SimplefunCommon.saveConfig` schrieb nicht atomar (`Files.writeString`, von REV24-N-4 übersehen) → `ConfigFiles.writeAtomic`.
- P4 `SimplefunCommon.getConfig()` ruft bei jedem Aufruf `normalize()` (u. a. aus `hurtServer` jedes Lebewesens) – einmal beim Laden/
  Setzen genügt. Config-Pfad `config/simplefun.json` relativ zum Arbeitsverzeichnis statt Loader-Config-Ordner. (REV2-A-9)
- P4 `mixin/KnockbackCapMixin` kappt Rückstoß (Vanilla-Höchstwert 2) auch bei ausgeschaltetem Sub-Mod – laut Test gewollt
  (Schutz gegen geschmiedete Verzauberungen), aber im Wiki als „wirkt immer“ vermerken. (REV2-A-9)

## simplehammers (114 Dateien, 35 Java, 63 JSON)
Geprüft: Hammer-Item (Umformen nur am Startblock der Ladung, Rechte `HammerProtection.mayTransform` = Zuschauer/Abenteuer/Spawn-
Schutz/`mayUseItemAt`/Claims, Diamant-/Quarz-Schläge nach der Rechteprüfung), Aufwertungen (`permitted` + `mayUseItemAt`, Aufträge je
Seite, Aufräumen bei Trennen), Flächenabbau (REV24-N-8 Gegenprobe: Level-Schlüssel), Amboss (REV24-F-09 Gegenprobe: Verträglichkeit
geprüft), Werkbank-Rest (Hammer nimmt 1 Schaden, Vanilla-Reste bleiben), 5 + 4 Client-Mixins (keine `@Redirect` mehr), Config.
- Kein P1–P3. P4 `engine/HammerAnvil.combine`: kombiniert beliebige zwei Hämmer (auch Stein + Netherit; Vanilla nur gleiche Items)
  immer für 1 Stufe – Verzauberungen billiger Hämmer wandern so ohne Stufenkosten auf teure. Entweder gleiche Stufe verlangen oder
  Kosten wie Vanilla nach Verzauberungen. (REV2-A-10)
- P4 `mixin/ShapelessRecipeMixin` überschreibt `getRemainingItems` für alle formlosen Rezepte (Konfliktgefahr mit anderen Mods, die
  dasselbe tun); enger wäre ein `@ModifyReturnValue` nur bei Hämmern im Raster. (REV2-A-10)
- P4 Tag `simplebuilding:craftable` (Kern, `mc26_3/overlay`) listet noch 7 `simplebuilding:*_sledgehammer` (laufen über IdMoves, im Gate
  grün) – auf `simplehammers:*` umstellen. (REV2-A-10)
- P4 `engine/SledgehammerProgress`: Liste mit `BLOCK.byNameCodec` strikt gelesen – ein Block einer entfernten Mod verwirft alle
  gespeicherten Schlag-Stände (nur Fortschritt, kein Item-Verlust). (REV2-A-10)

## simpleinterfaces (92 Dateien, 81 Java, 6 JSON)
Geprüft: Verknüpfte GUIs (REV24-M-4 Gegenprobe: Rechte jede Sekunde neu), tragbare Shulker/Endertruhe (Schreiben ins Item nur, solange
genau dieser Stapel im gesperrten Slot liegt; Sperre gegen Zifferntasten/Nebenhand/Doppelklick; Vanilla prüft `stillValid` vor
jedem Klick – kein Dupe gefunden), Netzpakete (nur `linked_cancel` vom Client, ohne Daten; Rest Server→Client mit Grenzen),
Endertruhen-Schnappschuss nur für den eigenen Spieler, 4 gemeinsame + 31 Client-Mixins (Seiten korrekt, Werkzeug-Abgleich aller
Mixin-Listen gegen die Klassen), Stile/Screens (nur Client), Config + Sync.
- Kein P1–P3. P4 `InterfacesFabric`: `SimpleInterfaces.onChange` hält wie bei simplefarming den alten Server nach dem Stopp. (REV2-A-11)

## simplelib (147 Dateien, 96 Java, 26 JSON)
Geprüft: API (IdMoves mit Schleifen-/Doppelschutz, InWorldUpgrades mit `mayBuild`/`mayInteract`, InWorldStrikes mit Tick-Aufräumen,
HammerActions, StackLimits/Hopper-Mixin, OwnedLights-Kehraus beim Chunk-Laden, PlayerState, LegendaryMoments, CarriedStacks,
AccessorySlots), Materialien (Anfrage-Prinzip, Loot nur ohne Partner), Amboss-Zerschlagen (Automatisierungs-Schutz), Warm-Essen,
Upgrade-Tabellen-Sync (Server→Client, 1 MiB je Datei), 19 + 7 Client-Mixins, Config.
- Kein P1–P3. P4 `api/CarriedStacks.drawable` verschluckt Fehler einer Quelle ganz still – einmal je Quelle loggen. (REV2-A-12)
- P4 `api/OwnedLights.sweep` geht bei jedem Chunk-Laden alle Marken der Dimension durch (Doku sagt „ein Set-Lookup“); bei vielen
  Trägern nach Chunk gruppieren. (REV2-A-12)
- P4 `mixin/WarmStackingMixin` macht `ItemStack.isSameItemSameComponents` global warm-blind; damit erkennt auch der Menü-Sync
  (`ItemStack.matches`) reine Wärme-Änderungen nicht – Anzeige der Restwärme kann im offenen Menü veralten. (REV2-A-12)

## simpleloot (26 Dateien, 15 Java, 6 JSON)
Geprüft: Tresor-Abklingzeit (Tick alle 40 Ticks, Klick-Reset, Stempel nur bei frischer Belohnung, Karte auf 128 Spieler wie Vanillas
`rewardedPlayers`, UUID-Schlüssel als String-Codec), Befehle (Stufe Owner, 1–36500 Tage), Config (QoL-Übernahme, atomar), Screen.
- Kein Fund.

## simplemaps (134 Dateien, 78 Java, 41 JSON)
Geprüft: alle 7 Client→Server-Pakete (Codec-Grenzen: 48 Kacheln, 32 Zeichen, 8 Quellen, 1 Kartenbild; Handler mit `enabled`,
Hand 0/1, Item neu aus der Hand gelesen, Drossel je Spieler), REV24-H-2 Gegenprobe (Kacheln nur für getragene/gerahmte Karten),
Rahmen-Speichern (Reichweite, fester Rahmen, `mayBuild`, Spawn-Schutz, Claims), Bildkarten (Rechte-Stufe, Größe, Farbbytes geprüft,
Minuten-Budget), Ebenen-Dienst (Rechteck geklammert, nur erkundete Chunks), Erkunden (nur geladene Chunks, nie Chunk-Laden),
Mega-Karte (Amboss nur Server, Kosten 1–39), Kartentisch-Mixin, Web-Karte (REV24-M-6 Gegenprobe; Dirty-Marken nur Server-Thread,
Obergrenze 2^18), 9 + 4 Client-Mixins, Config, Lang.
- **P2 behoben** `assets/simplemaps/lang/{en_us,de_de}.json`: alle 24 Texte des Rahmen-Bildschirms (`screen.simplemaps.frame.*`:
  Speichern, Bestätigen, Code, Kopieren, Status-/Fehlermeldungen) fehlten seit einem Merge nach 9e9e72ebd – der Bildschirm zeigte
  Rohschlüssel. Aus dem Ursprungs-Commit (inkl. `.help`-Umbenennung fd401385d) wiederhergestellt; neuer Wächter `tools/check_lang_keys.py`.
- P4 `net/MapsNetwork.handleTiles`: das Byte-Budget zählt nur Antworten; `data.render` läuft für bis zu 48 Kacheln je Paket (bei
  Zoom 64 je Kachel bis 4096 Nachschläge), auch wenn die Version gleich bleibt. Render-Aufrufe je Paket zählen (z. B. 16). (REV2-A-13)
