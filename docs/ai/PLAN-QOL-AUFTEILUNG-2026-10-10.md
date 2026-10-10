# Plan: Simple QoL wird Super-Mod mit Sub-Mods (2026-10-10)

Auftrag Besitzer 10.10. (Queue Nachtrag 14 / N32). Grundlagen: `docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`,
`docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`. Branch der ersten Stufen: `claude-q-qolsplit`.

## Entscheidungen (selbst getroffen, Besitzer kann umbenennen)

| Sub-Mod | Mod-ID | Inhalt (EN-Kurzname) |
|---|---|---|
| Simple Interfaces | `simpleinterfaces` | bisher `simplecontainers` (GUI-Stil) + Container-Verknüpfung + Easy Shulkers/Ender Chests aus QoL |
| Simple Movement | `simplemovement` | Kriechen, Autowalk, Leitern, Pulverschnee mit Frostläufer |
| Simple Farming | `simplefarming` | Ackerschutz, Schärfe mäht Gras, Hacken-Ernte |
| Simple Tools | `simpletools` | Haltbarkeitsbonus, Sparsamkeit (Thrift), Amboss-Kosten, Ofen-Lava-Füllung |
| Simple Creatures | `simplecreatures` | Stummschalten, Namensschild-Suffixe (stumm/Baby), Piglins ignorieren Gold |
| Simple Weather | `simpleweather` | Wetter aus (Server), Regendichte (Client) |
| Simple Vaults | `simplevaults` | Tresor-Abklingzeit (Tage) |

Simple QoL (`simplequalityoflife`) bleibt Super-Mod: Guide-Buch, FTB-Startquest, `/qol`-Befehle (soweit noch
nicht verteilt), gemeinsame Config-Oberfläche mit je Sub-Mod oberstem Punkt „Enable Simple XY“ /
„Simple XY aktivieren“. Mod-ID der Super-Mod bleibt (bestehende Welten/Configs).

## Inventar QoL → Ziel-Sub-Mod

| Feature / Config (`qOL.*`) | Klassen (shared, `com.simplequalityoflife.*`) | Ziel |
|---|---|---|
| `enableManualCrawl` | `PlayerEntityCrawlMixin`, `LivingEntityJumpMixin`, `util/Crawl*`, `CrawlStatePayload` | Movement |
| `enableAutowalk` | Client-Tick (QolFabricClient/NeoClient/ForgeClient + Keys) | Movement |
| `ladderClimbingSpeed`, `enableFastLadderSlide`, `ladderSlideSpeed`, `ladderSlideActivation` | `LadderSpeedMixin`, `ClimbPacketMixin`, `util/Climb*` | Movement |
| `frostWalkerWalkOnPowderSnow` (global) | `PowderSnowWalkMixin` | Movement |
| `preventFarmlandTrampleWithFeatherFalling` | `FarmlandTrampleMixin`, `util/Protection` | Farming |
| `sharpnessCutsGrass` | `SharpnessGrassCutMixin`, `GrassOutlineMixin`, `util/VegetationUtil` | Farming |
| `enableHoeHarvest` | `event/HoeHarvestHandler`, `HoeHarvestHint` (framework `TransformHints`) | Farming |
| `enableFullDurabilityBonus`, `fullDurabilityThreshold`, `fullDurabilityBonusMultiplier` | `FullDurability*Mixin` | Tools |
| Thrift-Verzauberung | `registry/Thrift`, `ThriftDurabilityMixin`, `data/.../enchantment/thrift.json`, Vanilla-Enchant-Tags | Tools |
| `anvilRepairKeepsCost` | `AnvilRepairCostMixin` | Tools |
| `enableFurnaceLavaFill` | `event/FurnaceLavaFillHandler` | Tools |
| `mutedEntities`, `nametagMuteSuffixes` | `EntityMuteMixin` | Creatures |
| `nametagBabySuffixes` | `PassiveEntityMixin` | Creatures |
| `piglinsIgnoreGoldTrims`, `piglinsIgnoreGoldTools` | `PiglinGoldMixin` | Creatures |
| `disableWeather` | `ServerWeatherMixin` | Weather |
| `clientRainParticleDensity` | `client/ClientWeatherMixin`, `client/VisualRainMixin` | Weather |
| `enableVaultCooldown`, `vaultCooldownDays` | `Vault*Mixin`, `Vault*Accessor`, `util/IVaultCooldown`, Befehl `vaultCooldown` | Vaults |
| `enableLinkedContainers`, `linkedContainerRange` | `container/Linked*`, `client/Linked*`, `LinkedOpenPayload`, `*LinkedMixin`, `SlotPositionAccessor` | Interfaces |
| `enableEasyShulkers`, `enableEasyEnderChests` | `container/Portable*`, `ItemUsePortableMixin` | Interfaces |
| Guide-Buch, FTB-Quest, Befehlswurzel, Config-Screen, `ConfigSyncPayload`, `InteractionGuard` | `guide/`, `command/`, `client/QolConfigScreen`, `network/ConfigSyncPayload`, `event/InteractionGuard` | bleibt Super-Mod (InteractionGuard wandert als Kopie mit, wo gebraucht – kein Klassenimport über Modulgrenzen) |

## Technik

- **Sub-Mod-Schalter:** `framework` bekommt den reinen Vertrag `com.simplebuilding.framework.api.SubMods`
  (`enabled(id)`, Standard an; `set(id, on)`). Die Super-Mod setzt ihn aus ihrer Config `enableSimpleXy`;
  die Sub-Mod fragt ihn an jeder Spielstelle ab. Allein geladen ist der Schalter immer an. Kein Import
  über Modulgrenzen (Regel 3), framework wird von jeder Sub-Mod selbst gebündelt.
- **Bündeln:** Super-Mod `requires` jede Sub-Mod identisch in `modules.json`, `fabric.mod.json` `depends`,
  NeoForge/Forge `type="required"`, und nestet sie wie simplelib: Fabric `include project(...)`, NeoForge
  `jarJar project(...)`; Forge: `ext.forgeBundleModules = ['simpleweather']` in `gradle/forge-framework.gradle`
  (Jar-in-Jar-Metadaten wie bei simplelib).
- **Config-Migration (einmalig):** Jede Sub-Mod hat eine eigene kleine JSON-Config
  (`config/<subid>.json`, Gson wie simpleinterfaces). Fehlt sie beim ersten Start, liest die Sub-Mod
  `config/simplequalityoflife.json` und übernimmt nur ihre `qOL.*`-Felder; danach schreibt sie ihre Datei
  (= Marker). Die alten Felder bleiben in der QoL-Datei liegen (Cloth ignoriert Unbekanntes nicht schädlich;
  sie werden beim nächsten Speichern der QoL-Config entfernt, weil das Feld nicht mehr existiert).
- **Umbenennung simplecontainers → simpleinterfaces:** Modulordner, Pakete
  `com.simplebuilding.modules.simpleinterfaces`, Namensräume `assets/`/`data/simpleinterfaces`,
  `simpleinterfaces.mixins.json`, Lang-Schlüssel, Wiki, Preise (`simplemoney/money/simpleinterfaces`),
  `integration/enabled-mods.json`, SB-/simplelib-/framework-Verweise (`isModLoaded`). Migration beim ersten
  Client-Start (`client/LegacyMigration`): `config/simplecontainers.json` → `config/simpleinterfaces.json`
  (nur wenn neu fehlt) und `options.txt` `key_key.simplecontainers.toggle_style` →
  `key_key.simpleinterfaces.toggle_style` (läuft vor dem Laden der Optionen: Fabric Client-Init,
  NeoForge/Forge `RegisterKeyMappingsEvent`). Lang braucht keine Migration (nur Ressourcen).
- **Tests:** je Sub-Mod Modul- und Standalone-Target (Fabric/NeoForge, Forge-Modultarget wo vorhanden),
  Super-Mod-Targets laufen mit den genesteten Sub-Mods.

## Stufen (jede für sich grün und merge-fähig)

1. **Plan** (diese Datei). ✔
2. **Umbenennung** simplecontainers → Simple Interfaces vollständig, inkl. Migration. ✔ (`claude-q-qolsplit`)
3. **Simple Weather** herauslösen (am wenigsten gekoppelt: 3 Mixins, 2 Optionen, 1 Test, keine
   Netzwerkpakete, keine Befehle) + `framework SubMods` + Super-Mod-Schalter „Simple Weather aktivieren“
   + Bündeln in QoL. ✔ (`claude-q-qolsplit`)
4. **Simple Vaults**: `Vault*Mixin`/Accessoren/`IVaultCooldown`, Befehl `vaultCooldown` in eigene
   `/simplevaults`-Wurzel (QoL-Befehl bleibt als Alias, solange QoL ihn kennt – ohne Import: Alias weg),
   Test `vault*` umziehen.
5. **Simple Creatures**: Mute/Baby/Piglin-Mixins, Listen-Normalisierung, Tests muting/baby/piglins.
6. **Simple Farming**: Farmland/Gras/Hacke; bündelt framework (`TransformHints`); `InteractionGuard`-Kopie.
7. **Simple Tools**: Haltbarkeit, Thrift (Registry + Enchant-Tags + Datapack-JSON, Namensraum
   `simpletools:thrift` – **Risiko** bestehende Verzauberungen `simplequalityoflife:thrift` in Welten:
   Thrift bleibt im Namensraum `simplequalityoflife`? Nein: Datenfix über Registry-Alias in der Sub-Mod
   prüfen; sonst Thrift in der Super-Mod lassen), Amboss, Ofen-Lava.
8. **Simple Movement**: Kriechen (Payload `CrawlStatePayload`), Autowalk-Taste (Keybind-Migration
   `key.simplequalityoflife.*` → `key.simplemovement.*`), Leitern inkl. Klettersicherheit.
9. **Linked Containers + Easy Shulkers → Simple Interfaces**: `container/*`, `client/Linked*`, Payload,
   Mixins; Server-Config `linkedContainerRange` (heute QoL, synchronisiert über `ConfigSyncPayload`) bekommt
   eigene Sync in simpleinterfaces (simpleinterfaces wird damit beidseitig statt rein Client).
10. **QoL aufräumen**: Config-Screen nur noch Sub-Mod-Schalter + Restoptionen, Guide-Seiten je Sub-Mod,
    Wiki-Struktur Super-/Sub-Mod.

## Risiken

- **Bestehende Configs:** einmalige Übernahme (oben); eine schon vorhandene Sub-Mod-Datei wird nie
  überschrieben. Server-Admins mit Befehlen `/qol tweak weather…`: gibt es nicht; spätere Stufen prüfen.
- **Doppelte Jars:** Spieler hat QoL (mit genesteter Sub-Mod) und die Sub-Mod einzeln → Loader wählt eine
  Version (Fabric JiJ, NeoForge/Forge Jar-in-Jar). Versionen der Sub-Mods getrennt pflegen.
- **Forge-Bündeln** anderer Module ist neu (bisher nur simplelib) – eigener Forge-Lauf nötig.
- **Mixin-Namensräume:** Präfix `qol$` bleibt in umgezogenen Mixins unkritisch; neue Präfixe je Sub-Mod.
- **Registry-IDs (Thrift, Guide-Buch)** wandern nicht ohne Datenfix (Stufe 7).
- **simpleinterfaces wird mit Stufe 9 serverseitig** (bisher reiner Client-Stil) → `environment` und
  Tests anpassen.
