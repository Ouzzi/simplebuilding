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
- Forge metadata template: forge/src/main/resources/META-INF/mods.toml
- NeoForge metadata template: neoforge/src/main/resources/META-INF/neoforge.mods.toml
- Resource expansion in subproject builds injects mod id and version at build time.
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

## Push/auth note
Push failed earlier because HTTPS auth token was missing/invalid.
Use either:
- GitHub CLI auth login, or
- SSH remote (git@github.com:...), or
- HTTPS with a valid PAT.

## Useful commands
- ./gradlew tasks
- ./gradlew :common:portStatus
- ./gradlew :forge:portStatus
- ./gradlew :neoforge:portStatus
