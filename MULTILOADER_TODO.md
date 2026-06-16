# Multiloader Migration – Test- & To-do-Status

_Erstellt: 2026-06-16. Grundlage: echter Gradle-Build aller Module + Subsystem-Lückenanalyse (Fabric-Wiring vs. NeoForge-Wiring)._

## 1. Build- & Testergebnis (tatsächlich ausgeführt)

| Modul | Befehl | Ergebnis |
|------|--------|----------|
| **Fabric** (root) | `gradlew :build` | ✅ Build + Test (`LanguageFilesTest`) grün |
| **NeoForge** | `gradlew :neoforge:build` | ✅ kompiliert + JAR, 1 Deprecation-Warnung |
| **common** | `gradlew :common:build` | ✅ grün |
| **Forge** | `gradlew :forge:build` | ❌ bricht ab (Toolchain-Konflikt, s. H1) |
| **Gesamt** | `gradlew build` | ❌ scheitert wegen Forge |

> ⚠️ **Wichtigster Vorbehalt:** Es wurde **nichts zur Laufzeit getestet** (kein `runClient` / `runServer` / `data` auf irgendeinem Loader ausgeführt). Alles unten basiert auf Build + Code-Analyse. NeoForge wurde **noch nie tatsächlich gestartet**.

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

- [ ] **B1 – Dedicated-Server-Crash durch Client-Mixin.** In `simplebuilding.client.mixins.json` steht `ClientSpeedometerMixin` (Ziel: client-only `ItemModelResolver`) im gemeinsamen `mixins`-Array statt im `client`-Array. Fabric gated die ganze Config auf `environment: client`; NeoForge lädt sie **beidseitig** → harter Crash beim Start eines NeoForge-**Dedicated-Servers**. (Singleplayer/Client laufen.)
  `simplebuilding.client.mixins.json:5-8` → verschieben nach `client:`-Array.
  _(Nebenbefund: `ItemMixin` im selben Array ist nur ein Code-Smell, kein Crash.)_

### 🟠 HIGH

- [ ] **H1 – `gradlew build` scheitert wegen Forge-Toolchain.** `forge/build.gradle` nutzt Java 21, hängt aber an `:common` (Java 25) → Dependency-Resolution-Fehler. Betrifft nur Tasks, die `:forge` anfassen; Fabric/NeoForge bauen einzeln sauber. Fix: Forge-Toolchain auf 25 anheben **oder** Forge-Modul entfernen (s. Entscheidung E1).
  `forge/build.gradle:50-55` vs `common/build.gradle:14-19`

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

- **E1 – Forge: fixen oder fallen lassen?** Forge für MC 26.x / 1.21.11 existiert praktisch nicht; das Modul ist ein leerer Scaffold (kein echtes `@Mod`, Dependency = `0.0.0`, nur `// TODO`). Optionen: (a) Modul entfernen → `gradlew build` wird sofort grün; (b) als deaktivierten Platzhalter behalten, aber Toolchain auf 25 anheben (H1); (c) später echte Forge-Portierung.
- **E2 – Server-Support nötig?** Wenn ja, ist B1 ein echter Blocker. Wenn nur Client/Singleplayer, hat B1 niedrigere Dringlichkeit.
- **E3 – Reihenfolge:** Vorschlag – zuerst NeoForge tatsächlich starten (`runClient`), dann B1/H1 fixen, dann M-Punkte.
