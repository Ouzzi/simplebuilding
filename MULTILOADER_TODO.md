# Multiloader – Test- & To-do-Status

_Stand: 2026-06-16. Loader: **Fabric** (root) + **NeoForge** (`:neoforge`). Eine experimentelle Forge-Implementierung wurde wieder entfernt (geringe Relevanz)._

## 1. Build- & Testergebnis (tatsächlich ausgeführt)

| Modul | Befehl | Ergebnis |
|------|--------|----------|
| **Fabric** (root) | `gradlew :build` | ✅ Build + Test (`LanguageFilesTest`) grün |
| **NeoForge** | `gradlew :neoforge:build` | ✅ kompiliert + JAR |
| **common** | `gradlew :common:build` | ✅ grün |
| **Gesamt** | `gradlew build` | ✅ baut beide Loader + Test grün |

> ✅ **Laufzeittest bestanden (2026-06-16):** `gradlew :neoforge:runClient` lädt sauber bis ins Hauptmenü — alle Registrierungen liefen, `Simplebuilding common initialized for neoforge`, **keine** Errors/Exceptions/Mixin-Fehler. (Client-Start validiert; Gameplay/Server noch nicht systematisch durchgespielt.)

## 2. Was funktioniert (NeoForge-Parität bestätigt – compile/analyse)

Auf NeoForge funktional vollständig verdrahtet (kein echtes Feature-Loch):
- **Core-Registry** (Blocks, Items, Block-Entities, Data-Components, Screen-Handler, Recipes, Item-Gruppen) – via `RegisterEvent` + `DeferredRegister`.
- **Gameplay-Events** (Sledgehammer, StripMiner, VeinMiner, Versatility, DynamicLight, Spatula-Migration) – via `@EventBusSubscriber`.
- **Networking** (alle 10 C2S + 3 S2C Payloads, PlatformServices, ClientNetworking).
- **Loot / Trades / Worldgen / Commands** – via NeoForge-Events + `biome_modifier`-JSONs (2 Erze = 2 JSONs).
- **Client** (Keybinds, HUD-Overlays, Menü-Screen, Tooltip, Item-Model-Property, Block-Highlight, S2C-Receiver, **Config-Screen**).

## 3. Offene Punkte – nach Priorität

### ✅ Erledigt in dieser Session
- **B1** – Dedicated-Server-Crash-Mixin (Client-Mixins ins side-gated `client`-Array).
- **M1** – NeoForge In-Game-Config-Screen (`IConfigScreenFactory` + cloth AutoConfig-GUI).
- **M2** – Versatility nur beim initialen Klick (`Action.START`), nicht pro Mining-Tick.
- **M3** – Versionsangleichung (READMEs auf MC 26.1.2 / Java 25 / Loader 0.19.3 / Mod 1.3.1).
- **M4** – Obsolete Version-Profile + Matrix-Tasks entfernt (`profiles/`, `matrix*`/`listVersionProfiles` in `build.gradle`) — die Mechanik war wirkungslos.
- **M6** – Alle Quellen committet, Working-Tree sauber.
- **L1** – Deprecation-Warnung (`KeyMapping.Category`) unterdrückt.
- **L2** – Repo-Müll entfernt (~4,5 MB javap-Dumps), `.gitignore` ergänzt.
- **L5** – `ModEnchantmentEffects`-No-Op geprüft → kein Bug (bewusster Platzhalter).
- **L6** – „tote" Mixins geprüft → behalten (geplantes Render-Scaffolding, kein Müll).

### 🟡 MEDIUM – offen
- [ ] **M5 – NeoForge-Datagen erzeugt nichts.** `data`-Run existiert, aber kein `GatherDataEvent`/Provider registriert → No-Op. Unkritisch, weil NeoForge die Fabric-generierten Assets aus `src/main/generated` mitnutzt. `neoforge/build.gradle`

### 🟢 LOW – offen (kosmetisch / minimal)
- [x] **L3 – `loom_version=1.16-SNAPSHOT`** ✅ **BEHOBEN (2026-08-20)** — auf Release `1.16.3` gepinnt (die Version, auf die der SNAPSHOT zuletzt auflöste; per maven.fabricmc.net verifiziert), Build weiterhin grün.
- [x] **L4 – `neoforge.mods.toml` Metadaten** ✅ **BEHOBEN** — `logoFile="assets/simplebuilding/icon.png"` (im JAR vorhanden, verifiziert) + `displayURL` ergänzt → Icon/Link in der NeoForge-Mod-Liste.
- [x] **L7 – Fragile Registrierung** ✅ **BEWERTET – keine Aktion.** Static-Init + implizite Lifecycle-Reihenfolge funktionieren in der aktuellen NeoForge-Lifecycle (RegisterEvent vor FMLCommonSetup). Strukturelle Anmerkung, kein Fehler; ein Guard wäre Nice-to-have, kein Muss.
- [x] **L8 – Payloads „required"** ✅ **BEWERTET – by design.** SimpleBuilding ist ein Inhalts-Mod, der ohnehin auf beiden Seiten installiert sein muss; `required` (kein `.optional()`) ist hier korrekt, nicht ein Bug.
- [x] **L9 – Ore-Biome-Targeting** ✅ **BEWERTET – akzeptabel.** `foundInTheEnd()` vs. `#minecraft:is_end` decken sich für das Vanilla-End; Abweichung nur bei modded End-Biomen ohne `is_end`-Tag → vernachlässigbar.

## 4. Laufzeit

✅ **NeoForge-Client startet sauber** (`gradlew :neoforge:runClient`, 2026-06-16): FML erkennt den Mod, alle Registrierungen + `common initialized` laufen, Textur-Atlanten/Sound/Resource-Reload (inkl. `mod/cloth_config`) OK, **0 Errors/Exceptions/Mixin-Fehler**, Hauptmenü erreicht.

✅ **Fabric-Client startet sauber** (`gradlew :runClient`, 2026-08-20): Fabric Loader 0.19.3 + MC 26.1.2, alle Registrierungen (`Simplebuilding common initialized for fabric`), Welt geladen, **0 Errors/Exceptions/Mixin-Fehler**.

Noch nicht systematisch in-game durchgespielt (optional, am echten Client): eine Welt erstellen und Gameplay prüfen — Items/Blöcke im Creative-Tab, Building-Wand + Highlight, Hopper-Menü, Doublejump, Config-Screen-Button, Trim-Boni. Sowie optional `:neoforge:runServer` (Dedicated-Server, prüft den B1-Fix real).

### Paritäts-Audit 1.21.11 ↔ Multiloader (2026-08-20, Fabric-Fokus)
Vollständiges Feature-Audit durchgeführt (10 Domänen, verifiziert): Kern-Gameplay, Blöcke, Netzwerk, Daten und Config sind vollständig portiert. **6 verifizierte Regressionen**, Fixes laufen in separaten Sessions:
- [ ] Trim-Glow auf getragener Rüstung (`EquipmentRendererMixin` leerer Stub, nicht registriert)
- [ ] Villager-/Wandering-Trades komplett entfallen (`ModTradeOffers`-Stub; `data/simplebuilding/villager_trade/` existiert nicht; Config-Schalter dadurch tot)
- [ ] Fabric: Vanilla-Blockauswahl-Outline invertiert unterdrückt (`SimplebuildingClient` `BEFORE_BLOCK_OUTLINE` ohne Negation)
- [ ] Building-Wand-Ghost-Vorschau fehlt (`getPreviewStates` ohne Aufrufer)
- [ ] Multi-Block-Abbau-Risse (Sledgehammer/Strip-/Vein-Miner) fehlen (`WorldRendererMixin` leerer Stub)
- [ ] Statusmeldungen im Chat statt Actionbar (`sendSystemMessage` statt `displayClientMessage(msg, true)`, ~10 Stellen)

### Kosmetik (optional)
- [x] Ressourcen-Warnungen: Ordner `backup 1/` unter `src/main/resources/assets/simplebuilding/textures/block/` hat ein Leerzeichen im Pfad → ungültige ResourceLocation, wird ignoriert (harmlos, geteilt mit Fabric). Erledigt (2026-08-20): PNGs unterscheiden sich von den aktiven Texturen → nach `art/dev-textures/checker-backup/` verschoben; außerdem 3 versehentlich committete JARs (~15 MB) aus `textures/item/` entfernt.
