# Audit simplestorage (b-audsb31-a, 11.11.2026)

Basis `hub/b-bundle182` 9b5764995, nur MC 26.3 Fabric. Aufbau wie `modules/simplemodels/AUDIT.md` (b-rel32).
Umfang des Mods: 167 Java-Dateien / 30,6k Zeilen (ohne Forge/NeoForge), davon ~45 % Tests. Ein Vollaudit Zeile fuer Zeile
war in einem Lauf nicht machbar; geprueft wurde **risikogeleitet** (P1-Klassen zuerst). Fruehere Audits, die hier nicht
wiederholt wurden: `docs/AUDIT-2026-09-26.md` (Nummern #17, #25, #32, #47 u. a. sind im Code vermerkt),
`docs/ai/AUDIT-A-2026-11-08.md` (Luecken/Dubletten, hub/a-audit09).

## Vollstaendig gelesen
- Netzwerk: `bundle/BundleNet.java` (2 C2S), `backpack/BackpackNet.java` (1 C2S), Registrierung in `StorageFabric.java`,
  `bundle/PlacedBundles#scroll`.
- Menues: `network/ControllerMenu.java`, `chest/TieredChestMenu.java` (+ `TieredChestSlot`), `pipe/PipeMenu.java`,
  `backpack/BackpackMenu.java` (Schnellverschieben, Lebenszyklus), `backpack/BackpackArmorSlot.java`, `backpack/WornBackpackContainer.java`.
- BlockEntities/Container: `pipe/ItemPipeBlockEntity.java` (Tick, push/pull), `crate/CrateBlockEntity.java`,
  `barrel/BarrelBlockEntity.java` (Laden/Speichern), `network/ChestNetwork.java` (Scan, Storage.insert/take),
  `network/ControllerChestBlockEntity.java` (Scan-Cache).
- Stichproben: `ReinforcedBundleItem` (Auswahl, Einlegen aus der Welt), `BackpackItem#mayStore`, `TieredShulkerBoxBlockEntity#canHold`,
  `ContainerCounts` (Komponente, Codec-Grenzen), statische Maps (Leaks).
- Nicht gelesen: Clients/Screens (ausser Pruefung, dass Logik serverseitig liegt), Fahrzeuge (`vehicle/`), `rare/`, Discover/Guide, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk-Payloads | 3 C2S. `SelectPayload`: nur Slot im eigenen offenen Menue, Bereich geprueft, Auswahl-Index von Vanillas `BundleContents.Mutable` begrenzt. `ScrollPayload`: Bauerlaubnis, Spawnschutz, geladen, `isWithinBlockInteractionRange`, Schleichen, Klang je Spieler gedrosselt. `OpenPayload`: Server waehlt den Rucksack selbst. Alles auf dem Server-Thread. |
| C2 Server-Autoritaet / Reichweite | Menues pruefen `stillValid` (Block-Entity-Abstand bzw. getragener Rucksack per Identitaet). Controller-Klicks, Sortieren, Suche rechnet nur der Server. |
| C3 Dupes in Menues/BEs | Uebergrosse Stapel (Stufen-Truhen) per eigenem `tryRemove`/`swapOversized`; Rucksack: Ruestungs-/Quellslot gesperrt, `syncFromSource` vor jedem Schreiben (Aenderungen von aussen gewinnen). Netzwerk: Einlegen nie in den Controller selbst. **P2-1, P2-2 gefunden und gefixt.** |
| C4 NBT/Komponenten | Fass laedt Slots mit Bereichs- und Mengenpruefung (`SafeCodecs.listLenient`), Kiste ebenso; `ContainerCounts` mit `intRange` und Listenlimit; Strings Vanilla-Limit 32767. Verschachtelung: Rucksack nimmt keine Rucksaecke/Shulker, Buendel keine Shulker (Vanilla-Regel). |
| C5 Tick-Kosten | Rohr: 1 Schritt je Abklingzeit, max. 4 Items. Netz: Scan gecacht (Versionszaehler), max. 512 Leiter. Controller-Liste alle 5 Ticks bei offenem Fenster (P3-1). |
| C6 Leaks | Statische Maps: nur Registries/Formen und `PlacedBundles.LAST_SCROLL` (ab 64 Eintraegen bereinigt). |
| C7 Schutz | `StorageProtection` (Admin-Bypass nur LEVEL_OWNERS), Buendel-Scroll mit `mayInteract`. |
| C8 Unabhaengigkeit | allein spielbar (Standalone-Target vorhanden), SimpleLib-Zugriffe ueber `com.simplelib.api`. |
| C9 Doku/Pflege | 52 Dubletten-Paare mit dem Kern (AUDIT-A); 7 Tooltip-Keys noch `tooltip.simplebuilding.*` (AUDIT-A). |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplestorage-P2-1 | P2 (Dupe-nah / Logik) | Das Rohr zog aus jedem Container ohne `Container#canTakeItem`: es leerte das Raster eines Autonomen Crafters (der Trichtern nichts gibt) und nahm einem Filter-Trichter (Simple Machines) sein festes Filter-Item. | **gefixt** d1fa2fb1b: Abfrage vor jedem Item (max. 4 je Schritt); Gametest `pipe_pull_respects_can_take_item` |
| simplestorage-P2-2 | P2 (Datenverlust, selten) | Controller: Schift-Klick auf einen Listeneintrag bei vollem Inventar legte den Rest per `storage.insert` zurueck und ignorierte, was nicht passte. Ein Alt-Item im Controller selbst (Index 0, kein Einlege-Ziel) verschwand, wenn alle Truhen voll waren. | **gefixt** 77bf250aa (+ Signatur-Fix): Rest faellt vor dem Spieler; Gametest `network_shift_take_with_full_inventory_loses_nothing` |
| simplestorage-P3-1 | P3 (Tick-Kosten) | `ControllerMenu.clickMenuButton` und jeder Zellenklick rechnen die ganze Liste sofort neu (`refresh`, 1-2x je Paket). Ein Client kann das per Paket-Spam vervielfachen; Kosten = Summe aller Slots des Netzes (max. Kisten der Stufe). | Liste: auf hoechstens 1 Neuberechnung je Tick drosseln (dirty-Flag + Tick-Stempel) |
| simplestorage-P3-2 | P3 | Rucksack -> Buendel -> Items mit grossen Komponenten (beschriebene Buecher) ergibt sehr grosse Item-Komponenten am getragenen Stapel - wie Vanillas Shulker+Buendel, keine eigene Grenze. | Hinweis |
| simplestorage-P3-3 | P3 | Controller-Schift-Klick bei vollem Inventar wirft den Rest jetzt ab statt ihn abzulehnen (Vanilla-Verhalten bei vollem Inventar). | Hinweis (bewusst) |
| simplestorage-P3-4 | P3 | 52 Kern-Dubletten (7979 Zeilen): jeder Fix muss im 26.2-Kern nachgezogen werden (die beiden Fixes hier betreffen nur Modul-Klassen ohne Kern-Paar). | Liste (REV24-F-17) |

## Urteil
**shipbar** (0 P1, 2 P2 gefixt, 4 P3). Der Mod ist durch fruehere Audits deutlich gehaertet (Payload-Pruefungen, Identitaetspruefung beim Rucksack, Codec-Grenzen).

## Testergebnis
Lokal: javac der geaenderten Dateien gegen 26.3 + Fabric-API (nur bekannte Access-Widener-Meldungen, keine in geaenderten Dateien),
`modules/simplestorage/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme. sbt-Lauf: siehe `docs/ai/PLAN-B-AUDSB31A-2026-11-11.md`.
Nicht getestet: Client, Forge/NeoForge, echter Mehrspieler-Paket-Spam.
