# Version Profiles

Use a profile by passing:

- `./gradlew :neoforge:loaderStatus -PversionProfile=neoforge-26-fixed`
- `./gradlew :neoforge:loaderStatus -PversionProfile=neoforge-26-latest`

These profile files override matching properties from gradle.properties during build script evaluation for Forge/NeoForge modules.

CI-like helper tasks in root build:

- `./gradlew matrixBaseline`
- `./gradlew matrixNeoforge26FixedStatus`
- `./gradlew matrixNeoforge26LatestStatus`
- `./gradlew matrixStatus`
