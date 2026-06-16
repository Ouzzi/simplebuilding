# Multiloader Migration – Test- & To-do-Status

_Erstellt: 2026-06-16. Grundlage: echter Gradle-Build aller Module + Subsystem-Lückenanalyse (Fabric-Wiring vs. NeoForge-Wiring)._

> **Loader-Rollen:** **Fabric** + **NeoForge** = aktuell / priorisiert. **Forge** = 🗄️ **Legacy** (baut mit, aber geringe Relevanz; Lücken in Abschnitt 5 werden bewusst nicht priorisiert). Siehe `MULTILOADER_MIGRATION.md`.

## 1. Build- & Testergebnis (tatsächlich ausgeführt)

| Modul | Befehl | Ergebnis |
|------|--------|----------|
| **Fabric** (root) | `gradlew :build` | ✅ Build + Test (`LanguageFilesTest`) grün |
| **NeoForge** | `gradlew :neoforge:build` | ✅ kompiliert + JAR, 1 Deprecation-Warnung |
| **common** | `gradlew :common:build` | ✅ grün |
| **Forge** | `gradlew :forge:build` | ✅ kompiliert + JAR (NEU implementiert, s. Abschnitt 5) |
| **Gesamt** | `gradlew build` | ✅ baut alle drei Loader (H1 behoben) |

> ⚠️ **Wichtigster Vorbehalt:** Es wurde **nichts zur Laufzeit getestet** (kein `runClient` / `runServer` / `data` auf irgendeinem Loader ausgeführt). Alles basiert auf Build + Code-Analyse. Weder NeoForge noch Forge wurde **je tatsächlich gestartet**.

> 🆕 **Update 2026-06-16:** Forge wurde implementiert (Commits `fc730e1`, `766c8e4`). Alle drei Loader (Fabric + NeoForge + Forge) bauen in **einem** Gradle-9.4.1-Build via ForgeGradle 7. Details + Forge-spezifische offene Punkte in **Abschnitt 5**.

## 2. Was funktioniert (NeoForge-Parität bestätigt)

Folgende Subsysteme sind auf NeoForge **funktional vollständig** verdrahtet (kein echtes Feature-Loch):
- **Core-Registry** (Blocks, Items, Block-Entities, Data-Components, Screen-Handler, Recipes, Item-Gruppen) – via `RegisterEvent` + `DeferredRegister`.
- **Gameplay-Events** (Sledgehammer, StripMiner, VeinMiner, Versatility, DynamicLight, Spatula-Migration) – via `@EventBusSubscriber`.
- **Networking** (alle 10 C2S + 3 S2C Payloads, PlatformServices, ClientNetworking).
- **Loot / Trades / Worldgen / Commands** – via NeoForge-Events + `biome_modifier`-JSONs (2 Erze = 2 JSONs).
- **Client** (Keybinds, HUD-Overlays, Menü-Screen, Tooltip, Item-Model-Property, Block-Highlight, S2C-Receiver) – alle korrekt portiert.

---

## 3. Offene Punkte – nach Priorität

### 🔴 BLOCKER

- [x] **B1 – Dedicated-Server-Crash durch Client-Mixin.** ✅ **BEHOBEN** — `ClientSpeedometerMixin` und `ItemMixin` vom gemeinsamen `mixins`-Array ins side-gated `client`-Array verschoben (`simplebuilding.client.mixins.json`). NeoForge/Forge-Dedicated-Server laden sie nun nicht mehr serverseitig. (Laufzeit noch ungetestet, aber die Crash-Ursache ist beseitigt.)

### 🟠 HIGH

- [x] **H1 – `gradlew build` scheitert wegen Forge-Toolchain.** ✅ **BEHOBEN** — `forge/build.gradle` komplett ersetzt (ForgeGradle 7, Java-25-Toolchain, echtes Forge 26.1.2). `gradlew build` baut jetzt alle drei Loader grün.

### 🟡 MEDIUM

- [ ] **M1 – Kein Config-Screen auf NeoForge.** AutoConfig lädt/speichert, aber es ist kein `IConfigScreenFactory` registriert (ModMenu ist Fabric-only). → Kein „Config"-Button in der NeoForge-Mod-Liste, GUI im Spiel nicht erreichbar. `SimplebuildingNeoForge.java:65-68` (ModContainer wird nie genutzt).
- [ ] **M2 – Versatility feuert bei NeoForge bei jedem Mining-Tick.** `onLeftClickBlock` filtert nicht auf `event.getAction()` → ggf. wiederholte Inventar-Swaps während des Abbauens, abweichend von Fabric. `NeoForgeGameplayEvents.java:43-52`
- [ ] **M3 – Inkonsistente Versionsnummern.** `gradle.properties` (26.1.2 / neo 26.1.2.75) vs. `profiles/*.properties` (1.21.11 / neo 26.1.2 ohne `.75`). Eine Seite ist falsch.
- [ ] **M4 – NeoForge ignoriert Version-Profile.** `neoforge/build.gradle` liest keine `profiles/*.properties` → `matrixStatus`/`-PversionProfile` schalten die NeoForge-Version **nicht** wirklich um (beworbenes Feature wirkungslos). `neoforge/build.gradle:8-14`
- [ ] **M5 – NeoForge-Datagen erzeugt nichts.** `data`-Run existiert, aber kein `GatherDataEvent`/Provider registriert → No-Op. Aktuell unkritisch, weil NeoForge die Fabric-generierten Assets aus `src/main/generated` mitnutzt. `neoforge/build.gradle:81-87`
- [ ] **M6 – ~635 uncommittete Änderungen.** Neue NeoForge-Quellen (`SimplebuildingNeoForge.java`, `NeoForgeGameplayEvents.java` …) sind **untracked** → Verlustrisiko. Empfehlung: bald committen.

### 🟢 LOW (Aufräumen / kosmetisch)

- [ ] **L1 – Deprecation:** `KeyMapping.Category.register(Identifier)` in `SimplebuildingNeoForgeClient.java:50` (Fabric versteckt es mit `@SuppressWarnings`). Rein kosmetisch.
- [ ] **L2 – Repo-Müll:** `tmp_items_javap.txt` (~2 MB), `tmp_*.txt`, `*.bu`, `compile-errors*.txt`, `build-*.log`, `.tmp-fabric/` sind weder ignoriert noch sinnvoll – `.gitignore` ergänzen + entfernen.
- [ ] **L3 – `loom_version=1.16-SNAPSHOT`** → auf Release-Version pinnen (Reproduzierbarkeit).
- [ ] **L4 – `neoforge.mods.toml`** ohne `logoFile`/URLs → kein Icon/Metadaten in der Mod-Liste.
- [ ] **L5 – `ModEnchantmentEffects.registerEnchantmentEffects()` ist auf BEIDEN Loadern ein No-Op** (Helper nie aufgerufen) – evtl. latenter Altbug, nicht NeoForge-spezifisch. Prüfen.
- [ ] **L6 – Tote Mixins** `WorldRendererMixin`, `EquipmentRendererMixin` (leer, in keiner Config) – entfernen.
- [ ] **L7 – Fragile Registrierung:** NeoForge verlässt sich auf Class-Loading-Seiteneffekte (Static-Init) und implizite Lifecycle-Reihenfolge in `assignStaticFields()`. Funktioniert, aber ohne Guard.
- [ ] **L8 – Payloads ohne `.optional()`** auf NeoForge → könnte Verbindungen mit abweichender Version ablehnen.
- [ ] **L9 – Ore-Biome-Targeting** `foundInTheEnd()` (Fabric) vs. `#minecraft:is_end` (NeoForge) – nur bei modded End-Biomen unterschiedlich.

---

## 4. Entscheidungen, die DU treffen musst

- **E1 – Forge: fixen oder fallen lassen?** ✅ **ENTSCHIEDEN/ERLEDIGT** — Forge 26.1.2 existiert doch (`net.minecraftforge:forge:26.1.2-64.0.9`) und wurde implementiert (ForgeGradle 7). Siehe Abschnitt 5.
- **E2 – Server-Support nötig?** Wenn ja, ist B1 ein echter Blocker (gilt für NeoForge **und** Forge). Wenn nur Client/Singleplayer, niedrigere Dringlichkeit.
- **E3 – Reihenfolge:** Vorschlag – jetzt alle drei Loader tatsächlich starten (`runClient`/`runServer`), B1 fixen, dann Forge-Client-Rest (Abschnitt 5) + M-Punkte.

---

## 5. Forge-Loader (🗄️ LEGACY) – Implementierungsstand (NEU, 2026-06-16)

> Forge wird als **Legacy** geführt. Es baut und funktioniert grundsätzlich, wird
> aber nicht aktiv gepflegt. Die offenen F-Punkte unten sind **niedrige Priorität**
> — sie blockieren die aktuellen Loader (Fabric/NeoForge) nicht.

**Tooling:** MinecraftForge 26.1.2 baut nur mit **ForgeGradle 7** (`[7.0.17,8)`), das als einziges FG Gradle 9 unterstützt. `:forge` ist ein Subprojekt im einheitlichen Build (FG7 koexistiert mit Loom + NeoForge-moddev). ForgeGradle 6 (Gradle 8) und ModDevGradle-legacyforge (nur ≤1.20.1) funktionieren **nicht** für 26.1.2 — empirisch bestätigt.

**✅ Implementiert & kompiliert (gegen echtes Forge 26.1.2 / EventBus 7):**
- Registries (Blocks/Items/BE/Menus/Recipes/CreativeTab) via `DeferredRegister`/`RegisterEvent`
- Gameplay-Events (Sledgehammer/StripMiner/VeinMiner/Versatility, DynamicLight, Spatula) via `@Mod.EventBusSubscriber`
- Loot, Commands, Worldgen (`forge:add_features`-Biome-JSONs)
- Networking (alle 10 C2S + 3 S2C Payloads) via `PayloadChannel`
- Client: Keybinds, Menü-Screen, Tooltip, Client-Tick (Doublejump/Spacekey), Login-Trim, Outline-Suppression
- `mods.toml` (+ Mixin-Configs), `@Mod`-Entry

**⚠️ Forge-spezifische offene Punkte / Risiken:**
- [ ] **F1 – Config nur Defaults.** cloth-config hat keinen Forge-Build für MC 26.x → ein minimaler `me.shedaniel.autoconfig`-Shim liefert nur Default-Werte (keine GUI, keine Persistenz). `forge/src/main/java/me/shedaniel/autoconfig/`
- [ ] **F2 – In-Welt-Build-Highlight fehlt.** Forge hat kein `RenderLevelStageEvent` → `BlockHighlightRenderer.renderInWorld` ist auf Forge nicht verdrahtet (Kern-Feature des Building-Wand). Braucht eigenen Forge-Render-Hook.
- [ ] **F3 – HUD-Overlays (Rangefinder/Speedometer) nicht verdrahtet.** Forges `AddGuiOverlayLayersEvent` hat andere API als NeoForge.
- [ ] **F4 – `enchant_type` Item-Model-Property nicht verdrahtet** (kein Forge-Registrierungs-Event; kosmetisch).
- [ ] **F5 – Laufzeit komplett ungetestet.** Besonders zu verifizieren: EventBus-7-Cancellation per `boolean`-Rückgabe (Gameplay/Highlight), `PayloadChannel`-Korrektheit, Loot ohne `HolderLookup.Provider` (`null` übergeben), `mods.toml`-Format.
