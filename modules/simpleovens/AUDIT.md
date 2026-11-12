# Audit simpleovens (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases).
Umfang: 25 Java-Dateien / 2,3k Zeilen ohne Tests (mit Tests 28 / 3,0k). Die Oefen sind Vanilla-Unterklassen
(`AbstractFurnaceBlockEntity`, Vanilla-Menues); geprueft wurde, was der Mod daran aendert. Aufbau wie `modules/simplestorage/AUDIT.md`.

## Vollstaendig gelesen
- BlockEntities (6 BE-Klassen/-Typen ueber 3 Familien): `block/TierFurnaceBlockEntity.java`, `TierSmokerBlockEntity.java`, `TierBlastFurnaceBlockEntity.java`
  (Menue, doppelte Erfahrung, Geschwindigkeit), dazu Vanillas `AbstractFurnaceBlockEntity#serverTick`/`setRecipeUsed`/Erfahrung (dekompiliert 26.3).
- Bloecke: `TierFurnaceBlock.java`, `TierSmokerBlock.java`, `TierBlastFurnaceBlock.java` (BE bleibt beim Stufenwechsel), `OvenUse.java`, `OvenItem.java`, `OvenBlock.java`.
- Regeln: `OvenPerks.java` (Zusatzticks, Erfahrung, Ausschluss-Tag), `OvenTier.java`, `config/OvensConfig.java` (Klemmung 1..8), `in_world_upgrades/ovens.json`.
- Mixin: `mixin/AbstractFurnaceMenuMixin.java` + `LongCookContainerData.java` (Menuedaten > 32767).
- Fahrzeuge: `cart/TieredFurnaceMinecart.java`, `cart/FurnaceMinecartItem.java`, `boat/OvenBoat.java`; `OvensRegistry.java` (Ausschnitte), `SimpleOvens.java`, `OvensFabric.java`.
- Nicht gelesen: `client/OvenBoatRenderer.java`, `OvensViewer`, Guide/Discover, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads; Vanilla-Ofenmenues. Menuedaten > 32767 werden vor dem Senden gemeinsam geteilt (Verhaeltnis bleibt), Schreiben unveraendert. |
| C2 Server-Autoritaet | Geschwindigkeit/Erfahrung nur im Server-Tick; Boot-Schub nur beim steuernden Spieler (Boote sind ohnehin client-autoritativ), Brennstoff serverseitig. |
| C3 Dupes/Verlust | Ergebnis-Slot, Hopper-Seiten, Abbau (Inhalt + gespeicherte Erfahrung) unveraendert Vanilla; Loot-Tabellen nur der Block. Doppelte Erfahrung zaehlt das Rezept zweimal, Ausgabemenge kommt immer aus dem Rezept. Stufen-Upgrade behaelt das BE (gleicher Typ, `shouldChangedStateKeepBlockEntity`). Minecart/Boot: Brennstoff wie Vanilla (1 Item je Klick). |
| C4 NBT | Vanilla-Laden; Boot klemmt `Fuel`, Minecart nicht (P3-3). |
| C5 Tick-Kosten | Ein zusaetzlicher `ContainerData`-Zugriff je Ofen und Tick; keine Suchen. |
| C6 Leaks | Keine statischen Sammlungen mit Spielern/Welten. |
| C7 Sicherheit/Rechte | Upgrade per Axt ueber SimpleLib (`mayBuild` + `mayInteract`), Hammer-Weg uebergibt an den Hammer. Mixin ohne `@Overwrite`, wirkt auf alle Ofenmenues (P3-4). |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel); Enderit-Stufe nur mit Simple Ores (Upgrade-Eintrag nennt `simpleores:enderite_nugget`). |
| C9 Doku/Pflege | Keine TODO/FIXME, Lang EN=DE (150/150); 3 Kern-Dubletten (`TieredFurnaceMinecart`, `AbstractFurnaceMenuMixin`, `LongCookContainerData`). |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simpleovens-P3-1 | P3 (Pflege) | `getSpeedMultiplier` in Hochofen/Raeucherofen ohne `@Override` (TierBlastFurnaceBlockEntity.java:49, TierSmokerBlockEntity.java:49): benennt Mojang die Methode um, laufen beide Stufen still mit halber Familien-Geschwindigkeit. | Empfehlung: `@Override` ergaenzen |
| simpleovens-P3-2 | P3 (Balance) | Doppelte Erfahrung (Netherit/Enderit) schliesst nur `#simpleovens:furnace_bonus_excluded` aus (gesprungener Diamant + optionaler SB-Tag); weitere verlustfreie Kreislaeufe anderer Module sind dort nicht geprueft. | Hinweis (neue Rezepte gegen den Tag pruefen) |
| simpleovens-P3-3 | P3 | `TieredFurnaceMinecart` laedt `Fuel` ungeklemmt (TieredFurnaceMinecart.java:174), das Boot klemmt (OvenBoat.java:186). Nur per NBT erreichbar. | Hinweis |
| simpleovens-P3-4 | P3 | `AbstractFurnaceMenuMixin` wickelt die Daten jedes Ofenmenues (auch Vanilla/Fremd-Mods), auch bei abgeschaltetem Modul; Wirkung nur oberhalb 32767 Ticks. | Hinweis (bewusst) |
| simpleovens-P3-5 | P3 | 3 Kern-Dubletten (`tools/dup_pairs.txt`): Fixes muessen im 26.2-Kern nachgezogen werden. | Liste |

## Urteil
**shipbar** (0 P1, 0 P2, 5 P3). Der Mod aendert an Vanillas Ofen nur Geschwindigkeit und Erfahrung; Container, Hopper und Abbau bleiben Vanilla.

## Testergebnis
Lokal: keine Code-Aenderung, daher kein javac-Lauf noetig. sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: Client, Forge/NeoForge, Boot-Schub im Mehrspieler.
