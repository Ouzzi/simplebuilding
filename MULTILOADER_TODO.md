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

### Paritäts-Audit 1.21.11 <-> Multiloader (2026-08-20/25, Fabric-Fokus)
Vollständiges Feature-Audit (10 Domänen, verifiziert): Kern-Gameplay, Blöcke, Netzwerk, Daten und Config sind vollständig portiert. **6 verifizierte Regressionen** — alle behoben und auf `claude/integration-check` zusammengeführt (`gradlew build` grün für beide Loader, Fabric-`runClient` sauber gebootet):
- [x] Trim-Glow auf getragener Rüstung — `EquipmentRendererMixin` neu implementiert + registriert (f854b8d); Mixin-Anwendung im Fabric-Log belegt.
- [x] Villager-/Wandering-Trades — 20 datengetriebene Trade-JSONs + 11 Tag-Merges, Loot-Funktion `simplebuilding:weighted_enchant`, Config-Gate via Resource-Condition (6f658db). **Fabric vollständig**; NeoForge siehe offene Punkte.
- [x] Fabric-Outline invertiert — Negation ergänzt (0e3421a).
- [x] Building-Wand-Ghost-Vorschau — `BuildingWandPreviewRenderer` (29e3ed6).
- [x] Multi-Block-Abbau-Risse — `MultiBlockBreakingSupport`, leerer `WorldRendererMixin` entfernt (29e3ed6).
- [x] Statusmeldungen Actionbar statt Chat — 13 Stellen auf `sendOverlayMessage` (1ce1d0d). **Fabric vollständig**; NeoForge siehe offene Punkte.
- [x] Nachgelagert gefunden: Vanilla-Outline wurde auf dem anvisierten Block ganz unterdrückt, obwohl beide Mod-Renderer ihn bewusst auslassen — 1.21.11-Verhalten wiederhergestellt (3f0a827).

Aufgeräumt: `backup 1/` nach `checker-backup/` außerhalb der Ressourcen verschoben + 3 verirrte Fremd-JARs entfernt (e253ac3) — **Mod-JAR 15,3 MB auf 1,2 MB**, Ressourcen-Fehlerspam beim Start weg.

### Offen aus dem Audit (NeoForge-seitig)
- [ ] **Config-Gate der Trades wirkt auf NeoForge nicht** — `fabric:load_conditions` wird dort ignoriert, `enableVillagerTrades`/`enableWanderingTrades` bleiben auf NeoForge wirkungslos. NeoForge-Pendant (eigene Condition oder Event-Filter) nötig.
- [ ] **Actionbar-Fix auf NeoForge unvollständig** — 3 Meldungen laufen dort weiterhin über den Chat (Constructor's Touch, Highlights-Toggle, Octant-Figure-Toggle), weil die NeoForge-Adapter eigene Kopien der Meldungen halten.
- [ ] **`ConfigResourceCondition` liegt im Paket `datagen`** — für NeoForge vom Quell-Filter ausgeschlossen; als Laufzeitklasse gehört sie nicht dorthin.

### Kosmetik (optional)
- [x] Ressourcen-Warnungen: Ordner `backup 1/` unter `src/main/resources/assets/simplebuilding/textures/block/` hat ein Leerzeichen im Pfad → ungültige ResourceLocation, wird ignoriert (harmlos, geteilt mit Fabric). Erledigt (2026-08-20): PNGs unterscheiden sich von den aktiven Texturen → nach `art/dev-textures/checker-backup/` verschoben; außerdem 3 versehentlich committete JARs (~15 MB) aus `textures/item/` entfernt.

## Forge: ZURUECKGESTELLT (Entscheidung 2026-08-27)

**Forge wird vorerst NICHT weiterverfolgt.** Das Modul bleibt im Repo und baut gruen
(`gradlew :forge:build`, MC 26.2 / Forge 26.2-65.1.3 / ForgeGradle 7.0.36), damit der
Wiedereinstieg jederzeit moeglich ist — es wird aber nicht auf Feature-Paritaet
gebracht und nicht ausgeliefert. Aktive Loader sind **Fabric und NeoForge**.

Was bei einem spaeteren Wiedereinstieg offen ist (belegt durch Vergleich mit dem
NeoForge-Modul):
- [ ] **Keiner der drei In-Welt-Renderer ist verdrahtet** — `BlockHighlightRenderer`,
  `BuildingWandPreviewRenderer` und `MultiBlockBreakingSupport` kommen im Forge-Modul
  nicht vor. Auf Forge fehlen damit Sledgehammer-/Octant-Highlights, die
  Building-Wand-Ghost-Vorschau und die Abbau-Risse auf Mehrfachbloecken.
  Erschwerend: MC 26.2 reicht Geometrie ueber `SubmitNodeCollector` ein; Forge 65.1.3
  hat kein offensichtliches Pendant zu NeoForges `SubmitCustomGeometryEvent`
  (Kandidat waere `AddFramePassEvent`, sonst ein eigener Mixin).
- [ ] **Kein Config-Screen** — NeoForge nutzt `IConfigScreenFactory` + cloth AutoConfig.
  `cloth-config` gibt es fuer Fabric und NeoForge, fuer Forge vermutlich nicht; dann
  waere `ForgeConfigSpec` + eigener Screen noetig. Achtung: `SimplebuildingConfig` und
  `HeldItemRendererMixin` liegen im gemeinsamen Baum und nutzen `me.shedaniel`-Klassen.
- [ ] **Weniger HUD-Verdrahtung als NeoForge** (`AddGuiOverlayLayersEvent`).
- [ ] **Nie zur Laufzeit gestartet** — auch nicht vor der Entfernung in dbdffdf.
  Ein `runClient`/`runServer`-Durchlauf steht komplett aus.
