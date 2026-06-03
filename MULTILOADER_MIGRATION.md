# Multiloader Migration (Fabric + Forge + NeoForge)

Current state:
- Root project remains the working Fabric build.
- New modules were added: common, forge, neoforge.
- This is an initial scaffold so existing Fabric work stays buildable while porting starts.

## What was set up
- settings.gradle now includes common, forge, neoforge.
- gradle.properties now contains placeholder properties:
  - forge_version
  - neoforge_version

## Recommended next migration steps
1. Keep root Fabric fully green (build + runClient).
2. Move cross-loader logic into common gradually:
   - registries, pure logic/util classes
   - networking abstractions
   - data/codec logic independent of loader APIs
3. Introduce loader adapters:
   - forge module for Forge-specific bootstrap/hooks
   - neoforge module for NeoForge-specific bootstrap/hooks
4. Pin actual loader versions in gradle.properties for your target MC line.
5. Add CI matrix jobs:
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
