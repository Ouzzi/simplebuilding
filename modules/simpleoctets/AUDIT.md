# Audit simpleoctets (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md`.
Umfang: 21 Java-Dateien / 2,5k Zeilen (ohne Tests), 1 Block-Entity (`MixedOctetBlockEntity`; die reinen Zellen sind BE-frei), 4 Mixins (3 echte + 1 Accessor),
keine Menues, **keine Payloads**. Die Zell-Logik (`OctetCellBlock`, `OctetCells`) ist oeffentliche API fuer Simple Building (Schach-Achtel); jeder Pfad
Platzieren/Herausnehmen/Abbauen wurde durchgespielt.

## Vollstaendig gelesen
- Zellen: `block/OctetCellBlock.java` (Abbau, Drops, Ecken-Abbau, Herausnehmen), `block/OctetCells.java` (Teile lesen/schreiben, Platzieren),
  `block/MixedOctetBlock.java`, `block/MixedOctetBlockEntity.java` (Laden/Speichern/Sync), `block/MaterialOctetBlock.java`, `item/MaterialOctetItem.java`.
- Mixins: `BlockCornerBreakMixin` (Block), `MelonSliceUseMixin` (Item#useOn), `MelonSliceTooltipMixin`, `FireBlockAccessor`; `MelonSlices.java`.
- Registrierung: `SimpleOctets.java`, `registry/OctetRegistry.java`, `registry/OctetMaterials.java` (Kopf; generiert), `OctetsFabric.java`; Rezepte
  (597, Verhaeltnis 1 Block = 8 Achtel geprueft); Client `client/MixedOctetRenderer.java`, `client/OctetBreakCracks.java`.
- Nicht gelesen: `discover/OctetsDiscover.java`, `guide/OctetsGuide.java` (generiert), `compat/jei/*`, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk-Payloads | Keine. Platzieren, Herausnehmen und Abbau laufen ueber Vanilla-Pakete; welcher Block und welches Achtel, bestimmt der Server aus Blickstrahl (`lookedAt`) und Trefferpunkt (`hitIndex`); das BE-Update schickt `getUpdatePacket` (kleiner Palette-Tag, hoechstens 8 Teile). |
| C2 Server-Autoritaet / Reichweite | Alle Weltaenderungen im `!level.isClientSide()`-Zweig (`OctetCells.place`, `useWithoutItem`); Reichweite durch Vanillas Paketpruefung (`mayInteract`, Spawnschutz/Weltrand), bei `lookedAt` mit `blockInteractionRange() + 1`. Client-Klassen nur in `client/`. |
| C3 Dupes / Verlust | Abbau durchgespielt (Vanilla 26.3 `ServerPlayerGameMode#destroyBlock`): `playerWillDestroy` merkt Rest/Achtel, `destroy` legt den Rest zurueck, `playerDestroy` -> `getDrops` liefert **nur** das abgebaute Achtel (`BREAKING` passt nur bei gleicher Ebene, Position und Tick; sonst haette die Zelle alles gedroppt und zugleich den Rest zurueckbekommen). Ohne Spieler (Explosion, Kolben) droppt jedes Achtel einmal, `noLootTable()` verhindert eine zweite Quelle. Mixed: `parts()` ist eine Kopie, `masked` veraendert die BE nicht; ohne BE keine Drops (Verlust, kein Dupe). Herausnehmen (Schleichen + leere Hand) ersetzt die Zelle zuerst, gibt dann das Item. Ecken-Abbau: nur mit Hammer, nicht bei Block-Entities, 7 Achtel bleiben, 1 faellt; Kreativ droppt nichts. 8 Achtel eines Materials werden zum Vollblock (Rezept 8 -> 1 gleich). Keine Container, keine Hopper-Seiten. |
| C4 NBT/Komponenten | `MixedOctetBlockEntity#loadAdditional`: Eintrag fuer Eintrag lesen (ein entferntes Material nimmt die anderen nicht mit), Index-Pruefung, unlesbare Teile leer; `dropGhostBits` raeumt Bits ohne Material nach dem Laden. Palette beim Laden ohne Listenlimit (P3-3). |
| C5 Tick-Kosten | Keine Ticks; Schatten (256 Formen) einmal beim Laden. |
| C6 Leaks | Nur `ThreadLocal BREAKING/AREA` (P3-1); keine Maps mit Spielern. |
| C7 Sicherheit/Rechte | Platzieren/Herausnehmen sind Rechtsklicks: Spawnschutz (`placed`: `mayInteract`), Abenteuer (`mayBuild`), Claims ueber die Haken von Simple Tweaks an `ServerPlayerGameMode#useItemOn` und `ItemStack#useOn` (das Achtel ist kein `BlockItem` -> strenge Pruefung, auch Nachbarzelle). Ecken-Abbau und Abbau laufen hinter Vanillas Abbau-Schutz (`destroyBlock` + Claims-Haken). Mixins: `BlockCornerBreakMixin` an `Block` (3 Haken, sofortiger Ausstieg ohne Schleichen + Hammer), `MelonSliceUseMixin` an `Item#useOn` (frueher Ausstieg), beide mit `SimpleOctets.on()`. |
| C8 Unabhaengigkeit | allein spielbar; Stonecutter-Rezepte nur ohne Simple Blocks (sonst dessen Shapecutter), Fremdmaterial nur mit dem Mod. |
| C9 Doku/Pflege | Keine TODO/FIXME; Lang EN=DE; 0 Kern-Dubletten. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleoctets-P3-1 | P3 (Aufraeumen) | `BREAKING` (`OctetCellBlock.java:298`) bleibt auf dem Server-Thread gesetzt, wenn kein `getDrops` folgt (Kreativ, falsches Werkzeug): `restoreRest` raeumt nur am Client (`:342`). Der Eintrag haelt Ebene und Teile, trifft aber nur bei gleicher Position und gleichem Tick, ist also wirkungslos. | Hinweis (`BREAKING.remove()` auch am Server nach `restoreRest`) |
| simpleoctets-P3-2 | P3 | Herausnehmen per Schleichen + leere Hand (`:439`) braucht kein Werkzeug und keine Abbauzeit (z. B. ein Obsidian-Achtel sofort). Kein Fortschrittsumgehen: Achtel entstehen nur ueber Rezepte, man bekommt, was man setzte. | Hinweis |
| simpleoctets-P3-3 | P3 | `MixedOctetBlockEntity#loadAdditional` liest die Palette ohne Laengengrenze (`:62`), nutzt aber nur 8 Indizes. | Hinweis |

## Urteil
**shipbar** (0 P1, 0 P2, 3 P3). Die Abbau-Pfade sind mit Gametests belegt (`sneaking_breaks_only_the_looked_at_octet`, `corner_break_*`, `mixed_cell_persists_and_drops_every_material`) und ergeben bei der Durchsicht keine Dupe-Luecke.

## Testergebnis
Lokal: nichts geaendert (kein Fix); `modules/simpleoctets/tools/check_data.py` gruen (202 Zellen, 597 Rezepte), `tools/check_lang_keys.py`, `check_tooltip_text.py` gruen, `check_dup_impl` 0 Probleme.
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`. Nicht getestet: kein neuer Test noetig; Client-Renderer, Forge/NeoForge, Mehrspieler-Gleichzeitigkeit.
