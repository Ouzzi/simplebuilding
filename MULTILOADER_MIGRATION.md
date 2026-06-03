# Multiloader Migration (Fabric + Forge + NeoForge)

Current state:
- Root project remains the working Fabric build.
- New modules were added: common, forge, neoforge.
- Forge and NeoForge modules now build as standalone Java subprojects with loader metadata templates.

## What was set up
- settings.gradle now includes common, forge, neoforge.
- gradle.properties now contains placeholder properties:
  - forge_version
  - neoforge_version
   - forge_loader_version_range
   - neoforge_loader_version_range
   - minecraft_version_range
- Forge metadata template: forge/src/main/resources/META-INF/mods.toml
- NeoForge metadata template: neoforge/src/main/resources/META-INF/neoforge.mods.toml
- Resource expansion in subproject builds injects mod id and version at build time.
- Forge/NeoForge metadata now also inject loader and Minecraft version ranges from gradle.properties.
- Common init class added: common/src/main/java/com/simplebuilding/common/SimplebuildingCommon.java
- Common bootstrap added: common/src/main/java/com/simplebuilding/common/SimplebuildingBootstrap.java
- Common startup plan added: common/src/main/java/com/simplebuilding/common/SimplebuildingStartupPlan.java
- Common loader enum added: common/src/main/java/com/simplebuilding/common/SimplebuildingLoader.java
- Fabric root now calls common init during startup.
- Loader Java entrypoint stubs added:
   - forge/src/main/java/com/simplebuilding/forge/SimplebuildingForgeEntrypoint.java
   - neoforge/src/main/java/com/simplebuilding/neoforge/SimplebuildingNeoForgeEntrypoint.java
- Loader bootstrap adapters added:
   - forge/src/main/java/com/simplebuilding/forge/SimplebuildingForgeModBootstrap.java
   - neoforge/src/main/java/com/simplebuilding/neoforge/SimplebuildingNeoForgeModBootstrap.java
- Verified successfully: ./gradlew :common:build :forge:build :neoforge:build

## Recommended next migration steps
1. Keep root Fabric fully green (build + runClient).
2. Move cross-loader logic into common gradually:
   - registries, pure logic/util classes
   - networking abstractions
   - data/codec logic independent of loader APIs
3. Introduce loader adapters:
   - forge module for Forge-specific bootstrap/hooks
   - neoforge module for NeoForge-specific bootstrap/hooks
4. Add real loader plugins and dependencies for Forge and NeoForge.
5. Port entrypoint/bootstrap classes for each loader.
6. Pin actual loader versions in gradle.properties for your target MC line.
7. Add CI matrix jobs:
   - fabric
   - forge
   - neoforge

## Useful commands
- ./gradlew tasks
- ./gradlew :common:portStatus
- ./gradlew :forge:portStatus
- ./gradlew :neoforge:portStatus
