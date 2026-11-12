# Audit simplewanderers (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric. Aufbau wie `modules/simplepads/AUDIT.md` (b-audsb31-a).
Umfang: 13 Java-Dateien / 1,5k Zeilen ohne Tests (Forge/NeoForge nicht Teil des Releases). Zusatzeingabe BEW39
(`/root/sb-tools-a/bewertung39/simplewanderers.json`, 1 rot). Vollstaendig gelesen (Mod ist klein).

## Vollstaendig gelesen
- Einstieg/Registrierung: `SimpleWanderers`, `WandererRegistry`, `WanderersFabric`, `WanderersFabricClient`, `client/VillageWandererRenderer`.
- Logik: `logic/WandererSpawner` (Besuch, Respawn, Fundort), `logic/WandererVillages` (SavedData), `logic/WandererTrades` (Ziehen, Perioden),
  `config/WandererConfig`.
- Entities: `entity/VillageWanderer` (Ziele, Rotation, Duplikat-Schutz, Tod, Pferd, Speichern), `entity/WandererHorse` (Einfangen, Leine).
- Daten: `data/minecraft/trade_set/wandering_trader/*` (vorher), `data/simplewanderers/trade_set/**`, Tags `villager_trade/**`, `tools/check_data.py`.
- Stichproben: `discover/WanderersDiscover`, `guide/WanderersGuide` (nur Hinweise/Buch, kein Netzwerk, keine Weltaenderung).

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | keine eigenen Payloads; Handel ueber das Vanilla-Haendlermenue. |
| C2 Server-Autoritaet | Spawn, Rotation, Pferd nur serverseitig; Config `simplewanderers-server.json` mit Grenzen (`clamp`), `/simplewanderers config` ueber SimpleLib. |
| C3 Dupes | Handel = Vanilla `MerchantMenu`; Rotation setzt Angebote nur ausserhalb eines Handels zurueck (`isTrading`). Pferde-Ruestung `DropChance 0`. |
| C4 NBT | `WandererVillages`: Liste begrenzt (`MAX_VILLAGES` 100 000, `listOf(0, MAX)`), Entity-Felder mit Standardwerten. |
| C5 Tick-Kosten | Besuch alle 200 Ticks je Spieler, POI-Suche hoechstens 1x je Minute je 8er-Zelle (`LAST`/`REVISIT`); Struktur-Abfrage nur bei geladenem Glocken-Chunk; `findSpot` laedt keine Chunks. Entity: Rotation 1x/s, Pferd alle 100 Ticks. |
| C6 Leaks | `MISSES` (je Dorf, beim Fund geraeumt), `LAST` (je Spieler-UUID, nie beim Ausloggen geraeumt, P3-2). |
| C7 Sicherheit/Rechte | keine Befehle ausser Config (SimpleLib, Operator); Toeten erlaubt (Besitzer W1-W10); Pferd einfangen per Schleichen + leere Hand (W4). |
| C8 Unabhaengigkeit | allein spielbar (Standalone-Ziel); Money/SB-Angebote nur als `required: false` in Tags. `modules.json requires` nannte SimpleLib nicht, obwohl `fabric.mod.json` sie verlangt → korrigiert (siehe P3-4). |
| C9 Doku | Vanilla-Tab-Einsortierung (Spawn-Eier/Werkzeuge) ist toter Code, weil SimpleLib `CreativeTabPolicy` Mod-Items aus Vanilla-Tabs nimmt (P3-3). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplewanderers-P2-1 | P2 (Logikfehler, BEW39 rot) | `shared/resources/data/minecraft/trade_set/wandering_trader/{common,buying}.json` ueberschrieben die Vanilla-Handelslisten des fahrenden Haendlers **ohne Bedingung**: mit `enabled=false` oder Super-Mod-Schalter aus spawnt kein Dorf-Wanderer mehr, der fahrende Haendler blieb aber um 47 Alltagswaren + 3 Ankaeufe gekuerzt – die Waren gab es dann nirgends. Eine Fabric-Ladebedingung hilft nicht: faellt sie, verwirft Fabric bei dynamischen Registern den ganzen Eintrag (kein Rueckfall auf Vanilla, `ResourceManagerRegistryLoadTaskMixin` gibt `null` zurueck). | **gefixt**: Vanilla-Sets bleiben unangetastet; die Haendler-Haelfte liegt als eigenes Set `simplewanderers:wandering_trader/{common,buying}`, `WanderingTraderSplitMixin` (`@ModifyArg` in `WanderingTrader#updateTrades`) waehlt sie nur bei `SimpleWanderers.on()` (`WandererTrades.traderSet`). Gametest `trader_split_follows_the_switch` (an: echter Haendler ohne Alltagsware; Config aus / Schalter aus: Vanilla-Sets), `vanilla_common_partition_complete` angepasst, `check_data.py` prueft „kein Vanilla-Override“. Mixin-Konfig auch in NeoForge-TOML/Forge-`forgeMixinConfigs` eingetragen (nicht kompiliert/getestet, NUR 26.3 Fabric). |
| simplewanderers-P3-1 | P3 (Kompat.) | Solange der Mod an ist, liest der fahrende Haendler fuer common/buying nur die eigene Tag-Haelfte: fremde Datenpakete/Mods, die `#minecraft:wandering_trader/common` oder `/buying` erweitern, fehlen bei ihm (SB/Money-Eintraege sind von Hand mit `required: false` nachgetragen). | Hinweis (Design der Aufteilung) |
| simplewanderers-P3-2 | P3 (Leak, klein) | `WandererSpawner.LAST` (UUID → Zelle/Zeit) wird beim Ausloggen nie geraeumt; waechst um einen Eintrag je Spieler, der je online war (16 Byte + Map-Eintrag). | Hinweis |
| simplewanderers-P3-3 | P3 (toter Code) | `WanderersFabric` sortiert Ei und Buch in Vanilla-Tabs ein; `CreativeTabPolicy` (IG31-3) entfernt Mod-Items dort wieder, weil der Mod einen eigenen Tab hat. | Hinweis |
| simplewanderers-P3-4 | P3 (Metadaten, BEW39 orange) | `modules/modules.json` `requires: []`, `fabric.mod.json` verlangt `simplelib >=0.4.0`. | **gefixt** (modules.json `requires: [simplelib]`, `docs/MODSTRUKTUR.*` neu erzeugt) |
| simplewanderers-P3-5 | P3 (Balance, BEW39 orange) | Das Wanderer-Pferd laesst sich einfangen (Besitzer W4) und kommt nach `respawnDays` neu – mit gefaerbter Lederpferderuestung, die man nach dem Zaehmen abnehmen kann (`DropChance 0` gilt nur fuer den Tod). Ein Pferd + Lederruestung je Dorf und 7 Tage. | Hinweis (gewollt laut W4; keine Frage, da gering) |

## Urteil
**shipbar nach Fix** (0 P1, 1 P2 gefixt, 5 P3, davon 1 gefixt).

## Testergebnis
Lokal: javac der geaenderten Dateien (`WandererTrades`, `WanderingTraderSplitMixin`, `WandererTests`, `ModuleGameTest`) – nur die bekannte
Access-Widener-Meldung in `simplelib/LibMaterials`; `tools/check_data.py` gruen; `tools/multimod.py` gueltig; `modtree.py` neu erzeugt.
sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md` (offen, sb-test nicht erreichbar).
Nicht getestet: Mixin-Anwendung im laufenden Spiel (erst im sbt), NeoForge/Forge (Mixin-Eintrag ohne Compile), Client.
