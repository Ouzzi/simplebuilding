# Multiloader – Test- & To-do-Status

_Stand: 2026-06-16. Loader: **Fabric** (root) + **NeoForge** (`:neoforge`). Eine experimentelle Forge-Implementierung wurde wieder entfernt (geringe Relevanz)._

## 1. Build- & Testergebnis (tatsächlich ausgeführt)

| Modul | Befehl | Ergebnis |
|------|--------|----------|
| **Fabric** (root) | `gradlew :build` | ✅ Build + Test (`LanguageFilesTest`) grün |
| **NeoForge** | `gradlew :neoforge:build` | ✅ kompiliert + JAR |
| **common** | `gradlew :common:build` | ✅ grün |
| **Gesamt** | `gradlew build` | ✅ baut beide Loader + Test grün |

> ⚠️ **Wichtigster Vorbehalt:** **NeoForge wurde noch nie tatsächlich gestartet** (kein `runClient` / `runServer` / `data`). Alles unten basiert auf erfolgreichem Build + Code-Analyse, nicht auf In-Game-Verhalten.

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
- [ ] **L3 – `loom_version=1.16-SNAPSHOT`** → auf Release-Version pinnen (Reproduzierbarkeit; vor dem Pinnen verfügbare Loom-Plugin-Version prüfen).
- [ ] **L4 – `neoforge.mods.toml`** ohne `logoFile`/URLs → kein Icon/Metadaten in der Mod-Liste.
- [ ] **L7 – Fragile Registrierung:** NeoForge verlässt sich auf Class-Loading-Seiteneffekte (Static-Init) und implizite Lifecycle-Reihenfolge in `assignStaticFields()`. Funktioniert, aber ohne Guard.
- [ ] **L8 – Payloads ohne `.optional()`** auf NeoForge → könnte Verbindungen mit abweichender Version ablehnen.
- [ ] **L9 – Ore-Biome-Targeting** `foundInTheEnd()` (Fabric) vs. `#minecraft:is_end` (NeoForge) – nur bei modded End-Biomen unterschiedlich.

## 4. Der eigentliche verbleibende Schritt

⚠️ **Laufzeittest.** Alle statischen/Build-/Analyse-Punkte sind erledigt. Was fehlt, ist die echte In-Game-Verifikation — NeoForge zum ersten Mal starten (`gradlew :neoforge:runClient`, ggf. `:neoforge:runServer`) und prüfen, dass Registrierung, Events, Networking, Config-Screen und Rendering tatsächlich laufen.
