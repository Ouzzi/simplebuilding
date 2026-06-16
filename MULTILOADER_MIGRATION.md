# Multiloader Setup (Fabric + NeoForge current · Forge legacy)

_Stand: 2026-06-16 (Minecraft 26.1.2)._

## Loader-Status

| Loader | Rolle | Modul | Tooling | Build |
|--------|-------|-------|---------|-------|
| **Fabric** | ✅ **aktuell / primär** | root (`src/main/java`) | Fabric Loom 1.16 | grün (+ Test) |
| **NeoForge** | ✅ **aktuell** | `:neoforge` | NeoForge ModDevGradle 2.0 | grün (JAR) |
| **Forge** | 🗄️ **Legacy / Best-Effort** | `:forge` | ForgeGradle 7 | grün (JAR) |

> **Forge ist Legacy.** Es baut mit und bleibt erhalten, hat aber heute geringe
> Relevanz. Aktiv gepflegt und als Zielplattform behandelt werden **Fabric** und
> **NeoForge**. Forge-spezifische Lücken (kein Config-GUI, kein In-Welt-Highlight,
> keine HUD-Overlays) werden bewusst nicht priorisiert — Details in
> `MULTILOADER_TODO.md`, Abschnitt 5.

## Architektur

- **root** = Fabric-Mod. Enthält den gesamten geteilten Spielcode unter
  `src/main/java/com/simplebuilding/**`.
- **`:common`** = winziger loader-agnostischer Bootstrap (`SimplebuildingCommon`,
  `SimplebuildingBootstrap`, `SimplebuildingStartupPlan`, `SimplebuildingLoader`).
- **`:neoforge`** und **`:forge`** rekompilieren denselben Root-Spielcode und
  liefern jeweils loader-spezifische Adapter (Entrypoint, Registries, Events,
  Networking, Client) sowie die Loader-Metadaten.
- Alle drei Loader bauen in **einem** Gradle-9.4.1-Build.

## Bauen

```
./gradlew build                 # baut alle drei Loader (Fabric + NeoForge + Forge)
./gradlew :build                # nur Fabric (root) + Test
./gradlew :neoforge:build       # nur NeoForge
./gradlew :forge:build          # nur Forge (Legacy)
```

JAR-Ausgaben:
- Fabric: `build/libs/simplebuilding-<version>.jar`
- NeoForge: `neoforge/build/libs/simplebuilding-neoforge-<version>.jar`
- Forge (Legacy): `forge/build/libs/simplebuilding-forge-<version>.jar`

## Wichtige Tooling-Fakten (warum es so aufgebaut ist)

- MinecraftForge 26.1.2 baut **nur** mit **ForgeGradle 7** (`[7.0.17,8)`); FG6
  unterstützt kein Gradle 9, ModDevGradle-legacyforge nur Forge ≤ 1.20.1.
- cloth-config hat **keinen** Forge-Build für die MC-26.x-Linie → das `:forge`-Modul
  bündelt einen minimalen `me.shedaniel.autoconfig`-Shim (nur Default-Werte).

## Laufzeit-Status

⚠️ **Noch kein Loader wurde tatsächlich gestartet** (kein `runClient`/`runServer`).
Alle Aussagen basieren auf erfolgreichem Build + Code-Analyse. Offene
Laufzeit-Verifikation und bekannte Lücken: siehe `MULTILOADER_TODO.md`.
