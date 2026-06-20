# Multiloader Setup (Fabric + NeoForge)

_Stand: 2026-06-16 (Minecraft 26.1.2)._

## Loader

| Loader | Modul | Tooling | Build |
|--------|-------|---------|-------|
| **Fabric** | root (`src/main/java`) | Fabric Loom 1.16 | grün (+ Test) |
| **NeoForge** | `:neoforge` | NeoForge ModDevGradle 2.0 | grün (JAR) |

> Es werden **nur Fabric und NeoForge** unterstützt. Eine frühere experimentelle
> Forge-Implementierung (ForgeGradle 7) wurde wieder **entfernt** (geringe Relevanz).

## Architektur

- **root** = Fabric-Mod. Enthält den gesamten geteilten Spielcode unter
  `src/main/java/com/simplebuilding/**`.
- **`:common`** = winziger loader-agnostischer Bootstrap (`SimplebuildingCommon`,
  `SimplebuildingBootstrap`, `SimplebuildingStartupPlan`, `SimplebuildingLoader`).
- **`:neoforge`** rekompiliert denselben Root-Spielcode und liefert die
  NeoForge-spezifischen Adapter (Entrypoint, Registries, Events, Networking,
  Client) sowie die `neoforge.mods.toml`.
- Beide Loader bauen in **einem** Gradle-9.4.1-Build.

## Bauen

```
./gradlew build               # baut beide Loader (Fabric + NeoForge) + Test
./gradlew :build              # nur Fabric (root) + Test
./gradlew :neoforge:build     # nur NeoForge
```

JAR-Ausgaben:
- Fabric: `build/libs/simplebuilding-<version>.jar`
- NeoForge: `neoforge/build/libs/simplebuilding-neoforge-<version>.jar`

## Laufzeit-Status

⚠️ **NeoForge wurde noch nicht tatsächlich gestartet** (kein `runClient`/`runServer`).
Alle Aussagen basieren auf erfolgreichem Build + Code-Analyse. Bekannte offene
Punkte: siehe `MULTILOADER_TODO.md`.
